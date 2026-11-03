package vn.laptop;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

public class Forms {
  @Getter
  @Setter
  public static class Register {
    @NotBlank
    @Email
    @Size(max = 160)
    public String email;

    @NotBlank
    @Size(min = 10, max = 60)
    public String password;

    @NotBlank
    @Size(max = 100)
    public String fullName;
  }

  @Getter
  @Setter
  public static class Profile {
    @NotBlank
    @Size(max = 100)
    public String fullName;

    @Pattern(regexp = "[0-9+ ()-]{0,20}")
    public String phone = "";

    @Size(max = 300)
    public String address = "";
  }

  @Getter
  @Setter
  public static class Checkout {
    @NotBlank
    @Size(max = 100)
    public String fullName;

    @NotBlank
    @Pattern(regexp = "[0-9+ ()-]{9,20}")
    public String phone;

    @NotBlank
    @Size(min = 10, max = 300)
    public String address;

    @Pattern(regexp = "COD|VNPAY|MOMO")
    @NotNull
    public String paymentMethod = "COD";

    @Size(max = 30)
    public String coupon = "";

    @NotBlank
    @Size(max = 64)
    public String token;
  }

  @Getter
  @Setter
  public static class ProductForm {
    public Long id;
    public Long version;

    @NotBlank
    @Size(max = 160)
    public String name;

    @NotBlank
    @Size(max = 60)
    public String brand;

    @NotNull public Long categoryId;

    @Pattern(regexp = "GAMING|OFFICE|GRAPHICS")
    @NotNull
    public String purpose = "OFFICE";

    @NotBlank
    @Size(max = 255)
    public String cpu;

    @NotBlank
    @Size(max = 255)
    public String ram;

    @NotBlank
    @Size(max = 255)
    public String storage;

    @NotBlank
    @Size(max = 255)
    public String gpu;

    @NotBlank
    @Size(max = 255)
    public String screen;

    @Size(max = 3000)
    public String description;

    @Min(1000)
    @Max(1000000000)
    public long price;

    @Min(0)
    @Max(1000000)
    public int stock;

    public boolean active = true;
  }
}
