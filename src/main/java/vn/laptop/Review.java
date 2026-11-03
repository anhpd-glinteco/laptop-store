package vn.laptop;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
    name = "reviews",
    uniqueConstraints = @UniqueConstraint(columnNames = {"accountId", "productId"}))
public class Review {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false)
  public Long accountId;

  @Column(nullable = false)
  public Long productId;

  @Column(nullable = false)
  public int rating;

  @Column(nullable = false, length = 1500)
  public String content;

  @Column(nullable = false)
  public String author;

  public java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();
}
