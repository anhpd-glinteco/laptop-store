# Laptop Store E-Commerce System

Hệ thống bán lẻ laptop trực tuyến xây dựng trên nền tảng Java Spring Boot, kiến trúc MVC server-side render với Thymeleaf, Bootstrap 5 và cơ sở dữ liệu MySQL. Hỗ trợ đa dạng phương thức thanh toán (COD, cổng thanh toán trực tuyến qua VNPay và MoMo sandbox), quản lý kho chống overselling bằng Pessimistic Locking và phân quyền người dùng theo vai trò.

---

## Tính năng chính

### Dành cho khách hàng (Customer)
- **Danh mục & Tìm kiếm**: Lọc sản phẩm theo hãng, nhu cầu sử dụng (Văn phòng, Gaming, Đồ họa), khoảng giá và sắp xếp linh hoạt.
- **Chi tiết sản phẩm**: Xem thông số kỹ thuật (CPU, RAM, GPU, ổ cứng, màn hình), tình trạng tồn kho thời gian thực và đánh giá từ người mua trước.
- **Giỏ hàng & Đặt hàng**: Thêm, cập nhật số lượng, áp dụng mã giảm giá, tính toán tổng tiền trực tiếp từ server để ngăn ngừa giả mạo giá.
- **Thanh toán linh hoạt**: Hỗ trợ thanh toán khi nhận hàng (COD), tích hợp adapter cổng thanh toán VNPay và MoMo.
- **Quản lý đơn hàng & Đánh giá**: Theo dõi tiến độ đơn hàng qua từng giai đoạn và gửi đánh giá sản phẩm sau khi đã nhận hàng thành công.

### Dành cho quản trị viên (Admin)
- **Báo cáo kinh doanh (Dashboard)**: Thống kê doanh thu thực tế theo ngày, tháng, năm hoặc khoảng thời gian tùy chọn; danh sách sản phẩm bán chạy nhất.
- **Quản lý sản phẩm & Kho hàng**: Thêm mới, chỉnh sửa thông số, cập nhật tồn kho, upload ảnh đại diện và ẩn sản phẩm (xóa mềm để bảo toàn dữ liệu lịch sử đơn hàng).
- **Quản lý danh mục**: Thao tác CRUD danh mục sản phẩm qua JDBC Template.
- **Quy trình xử lý đơn hàng**: Điều phối chuyển trạng thái đơn hàng theo quy trình khép kín: `Chờ duyệt` -> `Đang chuẩn bị` -> `Đang giao` -> `Đã giao`.

---

## Kiến trúc & Công nghệ

- **Backend**: Java 17+, Spring Boot 3.5.x
- **Bảo mật**: Spring Security 6 (Session-based Authentication, CSRF Protection, mã hóa mật khẩu BCrypt)
- **Truy cập dữ liệu**:
  - Spring Data JPA / Hibernate (Quản lý nghiệp vụ chính, locking, transaction)
  - Spring JDBC / JdbcTemplate (Minh họa CRUD danh mục và tổng hợp báo cáo doanh thu)
- **Frontend**: Thymeleaf, Bootstrap 5.3.3, CSS tùy chỉnh (Responsive hoàn chỉnh cho Desktop, Tablet và Mobile)
- **Cơ sở dữ liệu**: MySQL 8.x (Hỗ trợ profile H2 in-memory cho môi trường dev/test)

```text
Controller (Store / Admin / Api / Payment)
    └── Service Layer (Account, Catalog, Cart, Order, Review, Payment)
            └── Repository / DAO (JPA Repositories, JdbcCategoryDao, ReportDao)
                    └── Database (MySQL / H2)
```

---

## Yêu cầu môi trường

- **Java Development Kit (JDK)**: Phiên bản 17 hoặc 21
- **Apache Maven**: Phiên bản 3.6.3 trở lên
- **MySQL Server**: Phiên bản 8.0 trở lên (hoặc Docker Engine nếu chạy qua container)

---

## Hướng dẫn cài đặt & Khởi chạy

### 1. Khởi chạy nhanh (Chế độ Demo - Sử dụng H2 Database)
Không cần cài đặt trước MySQL, cơ sở dữ liệu H2 sẽ tự động khởi tạo dữ liệu mẫu:

```bash
cd laptop-store
mvn clean test
mvn spring-boot:run "-Dspring-boot.run.profiles=demo"
```

Truy cập ứng dụng tại: `http://localhost:8080`

**Tài khoản quản trị mặc định (Demo):**
- Email: `admin@laptop.local`
- Mật khẩu: `AdminDemo@123`

---

### 2. Khởi chạy với MySQL

#### Bước 2.1: Khởi tạo cơ sở dữ liệu
1. Mở MySQL Workbench hoặc MySQL CLI.
2. Thực thi script khởi tạo bảng: `database/schema.sql`.
3. Tạo tài khoản truy cập cho ứng dụng:

```sql
CREATE USER 'laptop_app'@'localhost' IDENTIFIED BY 'MatKhauUngDung@123';
GRANT SELECT, INSERT, UPDATE, DELETE ON laptop_store.* TO 'laptop_app'@'localhost';
FLUSH PRIVILEGES;
```

#### Bước 2.2: Cấu hình biến môi trường và chạy ứng dụng
Mở terminal trong thư mục `laptop-store`:

**Trên Windows (PowerShell):**
```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/laptop_store?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8"
$env:DB_USER="laptop_app"
$env:DB_PASSWORD="MatKhauUngDung@123"
$env:APP_SEED="true"
mvn spring-boot:run "-Dspring-boot.run.profiles=mysql"
```

**Trên Linux / macOS (Bash):**
```bash
export DB_URL="jdbc:mysql://localhost:3306/laptop_store?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8"
export DB_USER="laptop_app"
export DB_PASSWORD="MatKhauUngDung@123"
export APP_SEED="true"
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

---

### 3. Khởi chạy với Docker Compose (Tùy chọn)

Khởi động dịch vụ MySQL container:
```bash
docker compose up -d mysql
```
*(Cổng MySQL ngoài host được ánh xạ là `3307`, cổng container là `3306`)*

Chạy toàn bộ hệ thống (App + MySQL) trong container:
```bash
docker compose --profile full up -d --build
```

---

## Cấu trúc thư mục

```text
laptop-store/
├── src/
│   ├── main/
│   │   ├── java/vn/laptop/       # Mã nguồn Java (Controllers, Services, Repositories, Models)
│   │   └── resources/
│   │       ├── static/           # Tài nguyên tĩnh (CSS, JS, Hình ảnh minh họa)
│   │       └── templates/        # Giao diện Thymeleaf (Storefront và Admin portal)
│   └── test/java/vn/laptop/      # Bộ kiểm thử tích hợp (Integration Tests)
├── database/
│   └── schema.sql                # Kịch bản DDL khởi tạo cơ sở dữ liệu
├── docs/                         # Tài liệu kỹ thuật chi tiết
│   ├── THIET_KE.md               # Thiết kế chi tiết kiến trúc và dữ liệu
│   ├── USE_CASE.md               # Đặc tả các ca sử dụng
│   ├── KIEM_THU.md               # Kế hoạch và danh mục test cases
│   └── THANH_TOAN.md             # Đặc tả tích hợp cổng thanh toán
├── postman/                      # File cấu hình Postman API Collection
├── Dockerfile                    # Containerization build script
├── compose.yaml                  # Docker Compose orchestration
└── pom.xml                       # Maven build & dependencies definition
```

---

## Kiểm thử hệ thống

Dự án đi kèm bộ kiểm thử tích hợp toàn diện (24 test scenarios) kiểm tra:
- Khóa đơn hàng và tính chính xác của kho (Pessimistic Locking ngăn ngừa race conditions).
- Xác thực chữ ký thanh toán và cơ chế chống gửi lặp IPN (Idempotent Payment Callback).
- Phân quyền người dùng (Authorization, CSRF Protection, Role separation).
- Toàn vẹn giao dịch và tính nhất quán dữ liệu.

Chạy kiểm thử tự động:
```bash
mvn clean test
```
Toàn bộ kết quả chi tiết được lưu trữ tại `target/surefire-reports`.
