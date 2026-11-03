package vn.laptop;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SeedData implements CommandLineRunner {
  private final AccountRepository accounts;
  private final ProductRepository products;
  private final CategoryDao categories;
  private final PasswordEncoder encoder;

  @Value("${app.seed}")
  boolean seed;

  @Value("${app.admin-email}")
  String email;

  @Value("${app.admin-password}")
  String password;

  public SeedData(AccountRepository a, ProductRepository p, CategoryDao c, PasswordEncoder e) {
    accounts = a;
    products = p;
    categories = c;
    encoder = e;
  }

  @Transactional
  public void run(String... args) {
    if (!seed) return;
    if (password.length() < 10)
      throw new IllegalStateException(
          "Đặt ADMIN_PASSWORD tối thiểu 10 ký tự trước khi bật APP_SEED");
    if (accounts.findByEmail(email).isEmpty()) {
      Account a = new Account();
      a.email = email;
      a.fullName = "Quản trị Laptop Store";
      a.passwordHash = encoder.encode(password);
      a.role = "ADMIN";
      accounts.save(a);
    }
    if (categories.findAll().isEmpty()) {
      categories.create("Laptop mới");
      categories.create("Laptop cao cấp");
    }
    if (products.count() > 0) return;
    long cat = categories.findAll().get(0).id;
    String[][] data = {
      {
        "Dell Inspiron 15",
        "Dell",
        "OFFICE",
        "Intel Core i5",
        "16 GB",
        "512 GB SSD",
        "Intel Iris Xe",
        "15.6 inch FHD",
        "15990000"
      },
      {
        "Asus TUF Gaming A15",
        "Asus",
        "GAMING",
        "AMD Ryzen 7",
        "16 GB",
        "1 TB SSD",
        "NVIDIA RTX 4060",
        "15.6 inch 144 Hz",
        "27990000"
      },
      {
        "Apple MacBook Air M3",
        "Apple",
        "OFFICE",
        "Apple M3",
        "16 GB",
        "512 GB SSD",
        "Apple GPU",
        "13.6 inch Retina",
        "28990000"
      },
      {
        "Lenovo Legion 5",
        "Lenovo",
        "GAMING",
        "Intel Core i7",
        "32 GB",
        "1 TB SSD",
        "NVIDIA RTX 4060",
        "16 inch 165 Hz",
        "32990000"
      },
      {
        "Asus ProArt",
        "Asus",
        "GRAPHICS",
        "AMD Ryzen 9",
        "32 GB",
        "1 TB SSD",
        "NVIDIA RTX 4070",
        "16 inch OLED",
        "45990000"
      },
      {
        "Dell XPS 13",
        "Dell",
        "OFFICE",
        "Intel Core Ultra 7",
        "16 GB",
        "1 TB SSD",
        "Intel Arc",
        "13.4 inch FHD+",
        "36990000"
      }
    };
    for (var d : data) {
      Product p = new Product();
      p.name = d[0];
      p.brand = d[1];
      p.purpose = d[2];
      p.cpu = d[3];
      p.ram = d[4];
      p.storage = d[5];
      p.gpu = d[6];
      p.screen = d[7];
      p.price = Long.parseLong(d[8]);
      p.stock = 10;
      p.categoryId = cat;
      p.description =
          "Dữ liệu mẫu phục vụ học tập. Cấu hình và giá chỉ để demo, không phải báo giá thực tế.";
      products.save(p);
    }
  }
}
