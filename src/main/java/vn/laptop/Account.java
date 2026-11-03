package vn.laptop;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "accounts")
public class Account {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false, unique = true, length = 160)
  public String email;

  @Column(nullable = false)
  public String passwordHash;

  @Column(nullable = false, length = 100)
  public String fullName;

  public String phone = "";

  @Column(length = 300)
  public String address = "";

  @Column(nullable = false)
  public String role = "CUSTOMER";
}
