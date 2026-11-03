package vn.laptop;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {
  public record Line(Product product, int quantity) {
    public long getLineTotal() {
      return Math.multiplyExact(product.price, quantity);
    }
  }

  private final CartItemRepository carts;
  private final ProductRepository products;
  private final AccountRepository accounts;

  public CartService(CartItemRepository c, ProductRepository p, AccountRepository a) {
    carts = c;
    products = p;
    accounts = a;
  }

  public List<Line> lines(long uid) {
    return carts.findByAccountIdOrderByProductIdAsc(uid).stream()
        .map(c -> new Line(products.findById(c.productId).orElseThrow(), c.quantity))
        .toList();
  }

  public long total(long uid) {
    return lines(uid).stream().mapToLong(Line::getLineTotal).sum();
  }

  @Transactional
  public void change(long uid, long pid, int qty, boolean add) {
    accounts.locked(uid).orElseThrow();
    Product p = products.findById(pid).orElseThrow();
    if (!p.active && (add || qty != 0)) throw new IllegalArgumentException("Sản phẩm đã ngừng bán");
    CartItem c =
        carts
            .findByAccountIdAndProductId(uid, pid)
            .orElseGet(
                () -> {
                  CartItem n = new CartItem();
                  n.accountId = uid;
                  n.productId = pid;
                  return n;
                });
    if (qty < 0 || qty > 20) throw new IllegalArgumentException("Số lượng từ 0 đến 20");
    int n = add ? c.quantity + qty : qty;
    if (n > 20 || n > p.stock)
      throw new IllegalArgumentException("Số lượng vượt quá tồn kho hoặc giới hạn 20 máy");
    if (n == 0) {
      if (c.id != null) carts.delete(c);
    } else {
      c.quantity = n;
      carts.save(c);
    }
  }
}
