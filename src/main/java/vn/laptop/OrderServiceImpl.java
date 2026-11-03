package vn.laptop;

import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderServiceImpl implements OrderService {
  private final AccountRepository accounts;
  private final ProductRepository products;
  private final CartItemRepository carts;
  private final ShopOrderRepository orders;
  private final OrderItemRepository items;
  private final PaymentGateway gateway;

  public OrderServiceImpl(
      AccountRepository a,
      ProductRepository p,
      CartItemRepository c,
      ShopOrderRepository o,
      OrderItemRepository i,
      PaymentGateway g) {
    accounts = a;
    products = p;
    carts = c;
    orders = o;
    items = i;
    gateway = g;
  }

  @Transactional
  public ShopOrder checkout(long uid, Forms.Checkout f) {
    accounts.locked(uid).orElseThrow();
    var old = orders.findByCheckoutToken(f.token);
    if (old.isPresent()) {
      if (old.get().accountId != uid) throw new IllegalArgumentException("Token không hợp lệ");
      return old.get();
    }
    if (!Set.of("COD", "VNPAY", "MOMO").contains(f.paymentMethod))
      throw new IllegalArgumentException("Phương thức không hợp lệ");
    if (!gateway.enabled(f.paymentMethod))
      throw new IllegalArgumentException("Chưa cấu hình thanh toán này. Hãy chọn COD");
    var cart = carts.findByAccountIdOrderByProductIdAsc(uid);
    if (cart.isEmpty()) throw new IllegalArgumentException("Giỏ hàng trống");
    ShopOrder o = new ShopOrder();
    o.accountId = uid;
    o.checkoutToken = f.token;
    o.fullName = f.fullName;
    o.phone = f.phone;
    o.address = f.address;
    o.paymentMethod = f.paymentMethod;
    var lines = new ArrayList<OrderItem>();
    for (var c : cart) {
      Product p = products.locked(c.productId).orElseThrow();
      if (!p.active || c.quantity < 1 || c.quantity > 20 || p.stock < c.quantity)
        throw new IllegalArgumentException("Không đủ tồn kho: " + p.name);
      p.stock -= c.quantity;
      OrderItem i = new OrderItem();
      i.productId = p.id;
      i.productName = p.name;
      i.unitPrice = p.price;
      i.quantity = c.quantity;
      o.subtotal = Math.addExact(o.subtotal, Math.multiplyExact(p.price, c.quantity));
      lines.add(i);
    }
    o.coupon = f.coupon == null ? "" : f.coupon.trim().toUpperCase(Locale.ROOT);
    if (!o.coupon.isEmpty() && !o.coupon.equals("LAPTOP5"))
      throw new IllegalArgumentException("Mã giảm giá không hợp lệ");
    o.discount = o.coupon.equals("LAPTOP5") ? Math.min(o.subtotal * 5 / 100, 500000) : 0;
    o.total = o.subtotal - o.discount;
    if (!f.paymentMethod.equals("COD")) {
      o.expiresAt = LocalDateTime.now().plusMinutes(15);
      o.paymentRef = UUID.randomUUID().toString().replace("-", "");
    }
    orders.saveAndFlush(o);
    for (var i : lines) {
      i.orderId = o.id;
      items.save(i);
    }
    carts.deleteByAccountId(uid);
    return o;
  }

  public ShopOrder owned(long uid, long id) {
    ShopOrder o =
        orders.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn"));
    if (o.accountId != uid)
      throw new org.springframework.security.access.AccessDeniedException(
          "Không có quyền xem đơn hàng");
    return o;
  }

  @Transactional
  public void transition(long id, String next) {
    ShopOrder o = orders.locked(id).orElseThrow();
    String expected =
        switch (o.status) {
          case "NEW" -> "PREPARING";
          case "PREPARING" -> "SHIPPING";
          case "SHIPPING" -> "DELIVERED";
          default -> "";
        };
    if (!expected.equals(next) || expected.isEmpty())
      throw new IllegalArgumentException("Thứ tự hợp lệ: NEW → PREPARING → SHIPPING → DELIVERED");
    if (!o.paymentMethod.equals("COD") && !o.paymentStatus.equals("PAID"))
      throw new IllegalArgumentException("Đơn trực tuyến chưa thanh toán");
    o.status = next;
    if (next.equals("DELIVERED")) {
      o.deliveredAt = LocalDateTime.now();
      if (o.paymentMethod.equals("COD")) {
        o.paymentStatus = "PAID";
        o.paidAt = LocalDateTime.now();
      }
    }
  }

  @Transactional
  public void cancel(long uid, long id) {
    ShopOrder o = orders.locked(id).orElseThrow();
    if (o.accountId != uid)
      throw new org.springframework.security.access.AccessDeniedException("Không có quyền");
    if (!o.status.equals("NEW") || !o.paymentMethod.equals("COD"))
      throw new IllegalArgumentException("Chỉ hủy đơn COD chưa duyệt; đơn online cần đối soát");
    release(o);
    o.status = "CANCELLED";
    o.paymentStatus = "CANCELLED";
  }

  private void release(ShopOrder o) {
    for (var i : items.findByOrderIdOrderByProductIdAsc(o.id)) {
      Product p = products.locked(i.productId).orElseThrow();
      p.stock += i.quantity;
    }
  }

  @Transactional
  public String paymentResult(
      String ref, String provider, long amount, boolean success, String transaction) {
    var found = orders.lockRef(ref);
    if (found.isEmpty()) return "01";
    ShopOrder o = found.get();
    if (!o.paymentMethod.equals(provider) || o.total != amount) return "04";
    if (Set.of("PAID", "REVIEW_REQUIRED").contains(o.paymentStatus)) return "02";
    if (success) {
      if (!o.status.equals("NEW")) {
        o.paymentStatus = "REVIEW_REQUIRED";
        o.providerTransaction = transaction;
        return "00";
      }
      o.paymentStatus = "PAID";
      o.paidAt = LocalDateTime.now();
      o.providerTransaction = transaction;
    } else if (o.paymentStatus.equals("PENDING") && o.status.equals("NEW")) {
      release(o);
      o.paymentStatus = "FAILED";
      o.status = "CANCELLED";
    } else return "02";
    return "00";
  }

  @Transactional
  public void expire(long id) {
    ShopOrder o = orders.locked(id).orElseThrow();
    if (o.status.equals("NEW")
        && o.paymentStatus.equals("PENDING")
        && !o.paymentMethod.equals("COD")
        && o.expiresAt.isBefore(LocalDateTime.now())) {
      release(o);
      o.status = "EXPIRED";
      o.paymentStatus = "EXPIRED";
    }
  }
}
