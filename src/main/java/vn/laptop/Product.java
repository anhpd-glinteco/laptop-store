package vn.laptop;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "products")
public class Product {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false, length = 160)
  public String name;

  @Column(nullable = false, length = 60)
  public String brand;

  @Column(nullable = false)
  public Long categoryId;

  @Column(nullable = false, length = 30)
  public String purpose;

  public String cpu;
  public String ram;
  public String storage;
  public String gpu;
  public String screen;

  @Column(length = 3000)
  public String description;

  @Column(nullable = false)
  public long price;

  @Column(nullable = false)
  public int stock;

  public String imageUrl = "/img/laptop.svg";

  @Column(nullable = false)
  public boolean active = true;

  @Version public Long version;
}
