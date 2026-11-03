package vn.laptop;

import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
  private final ShopOrderRepository orders;
  private final PaymentGateway gateway;

  public PaymentService(ShopOrderRepository o, PaymentGateway g) {
    orders = o;
    gateway = g;
  }

  // Kept under the order lock so two payment buttons cannot create two sessions.
  // Network timeout is bounded by PaymentGateway; order creation already committed separately.
  @Transactional
  public String start(long uid, long id, String ip) {
    ShopOrder o = orders.locked(id).orElseThrow();
    if (o.accountId != uid)
      throw new org.springframework.security.access.AccessDeniedException("Không có quyền");
    if (o.paymentMethod.equals("COD")
        || !o.status.equals("NEW")
        || !o.paymentStatus.equals("PENDING")
        || o.expiresAt.isBefore(LocalDateTime.now()))
      throw new IllegalArgumentException("Đơn không còn chờ thanh toán");
    if (o.paymentUrl == null) o.paymentUrl = gateway.url(o, ip);
    return o.paymentUrl;
  }
}
