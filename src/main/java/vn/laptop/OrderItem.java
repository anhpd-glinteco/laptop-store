package vn.laptop;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "order_items")
public class OrderItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false)
  public Long orderId;

  @Column(nullable = false)
  public Long productId;

  @Column(nullable = false)
  public String productName;

  @Column(nullable = false)
  public long unitPrice;

  @Column(nullable = false)
  public int quantity;

  public long getLineTotal() {
    return unitPrice * quantity;
  }
}
