package vn.laptop;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "shop_orders")
public class ShopOrder {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false)
  public Long accountId;

  @Column(nullable = false, unique = true, length = 64)
  public String checkoutToken;

  @Column(nullable = false, length = 40)
  public String status = "NEW";

  @Column(nullable = false, length = 30)
  public String paymentMethod;

  @Column(nullable = false, length = 40)
  public String paymentStatus = "PENDING";

  @Column(nullable = false)
  public long subtotal;

  public long discount;
  public long total;
  public String coupon = "";

  @Column(nullable = false, length = 100)
  public String fullName;

  @Column(nullable = false, length = 20)
  public String phone;

  @Column(nullable = false, length = 300)
  public String address;

  @Column(nullable = false)
  public java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();

  public java.time.LocalDateTime paidAt;
  public java.time.LocalDateTime deliveredAt;
  public java.time.LocalDateTime expiresAt;

  @Column(unique = true, length = 64)
  public String paymentRef;

  @Column(length = 2000)
  public String paymentUrl;

  public String providerTransaction;
  @Version public Long version;
}
