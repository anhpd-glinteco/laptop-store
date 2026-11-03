# Laptop Store — Dự án TX1 trên VS Code

Ứng dụng học tập Java Spring Boot + Thymeleaf + Bootstrap + MySQL. Frontend render phía server; không cần Node.js hay React. Có chế độ H2 chạy thử không cần Docker. COD chạy độc lập; VNPay/MoMo là adapter sandbox cần merchant thật để kiểm tra đầu-cuối.

## 1. Đọc theo thứ tự

1. README này: chạy dự án, cấu hình MySQL, thao tác demo.
2. `docs/THIET_KE.md`: phân tích chức năng, kiến trúc, mô hình dữ liệu, quy tắc nghiệp vụ.
3. `docs/USE_CASE.md`: đặc tả và ngoại lệ.
4. `docs/uml/`: 4 Use Case Diagram chuẩn PlantUML và 2 Sequence Diagram.
5. `docs/KIEM_THU.md`: kiểm thử tự động, thủ công, Postman và giới hạn xác minh.
6. `docs/THANH_TOAN.md`: cấu hình sandbox và xác thực IPN.
7. `docs/DEMO_VA_NOP_BAI.md`: kịch bản 20 phút và checklist nộp.

## 2. Công cụ cần có

| Công cụ | Phiên bản / mục đích |
|---|---|
| JDK | 17 hoặc 21; mã nguồn target Java 17 |
| Apache Maven | 3.6.3 trở lên; đã dùng Maven 3.9.11 để kiểm tra |
| Spring Boot | 3.5.16, cố định trong pom.xml |
| VS Code | Extension Pack for Java; Spring Boot Extension Pack |
| MySQL | 8.x, đề xuất 8.4; hoặc Docker Compose |
| Postman | Import file `postman/LaptopStore.postman_collection.json` |
| Git | Quản lý phiên bản, không commit mật khẩu |

Spring Boot 3.5 yêu cầu tối thiểu Java 17 và Maven 3.6.3. Nguồn: https://docs.spring.io/spring-boot/3.5/system-requirements.html

Mở PowerShell mới sau khi cài JDK/Maven:

```powershell
java -version
javac -version
mvn -v
git --version
```

`mvn -v` cần trỏ tới JDK 17/21. Nếu báo không nhận lệnh, thêm thư mục `bin` của Maven vào PATH rồi mở lại VS Code. Không cần cài Tomcat riêng.

## 3. Chạy nhanh trên VS Code, chưa cần MySQL / Docker

1. Giải nén ZIP vào thư mục đơn giản, ví dụ `D:\Projects\laptop-store`.
2. VS Code → File → Open Folder → chọn thư mục chứa `pom.xml`.
3. Cài các extension được đề xuất. Chờ Java tải và lập chỉ mục dependency.
4. Terminal → New Terminal → PowerShell.
5. Chạy:

```powershell
mvn clean test
mvn spring-boot:run "-Dspring-boot.run.profiles=demo"
```

6. Chờ dòng `Started LaptopApplication` rồi mở http://localhost:8080.
7. Tài khoản admin **chỉ cho demo cục bộ**:
   - Email: `admin@laptop.local`
   - Mật khẩu: `AdminDemo@123`
8. Đăng ký một tài khoản khách để demo luồng mua hàng.

Chế độ demo dùng H2 lưu tại `data/laptop-demo.*`, **không phải MySQL**, không thay thế bước chứng minh JDBC/MySQL khi nộp bài. Bootstrap được đóng gói bằng WebJar, không phụ thuộc CDN. Các sản phẩm seed dùng ảnh minh họa chung, cấu hình và giá giả lập cho học tập.

Dừng server: Ctrl+C. Mở hai server cùng lúc trên cổng 8080 sẽ lỗi. Đổi cổng nếu cần:

```powershell
$env:PORT="8081"
mvn spring-boot:run "-Dspring-boot.run.profiles=demo"
```

Đổi password admin trước lần seed đầu tiên:

```powershell
$env:ADMIN_PASSWORD="MatKhauRiengCuaBan@123"
mvn spring-boot:run "-Dspring-boot.run.profiles=demo"
```

Seed không ghi đè tài khoản đã tồn tại, nên đổi biến môi trường sau lần chạy đầu sẽ không đổi mật khẩu trong DB.

## 4. Chạy MySQL trực tiếp, không cần Docker Desktop

Dùng MySQL Server + Workbench hoặc MySQL CLI. SQL Server không thay cho MySQL trong cấu hình này.

1. Tạo kết nối MySQL bằng tài khoản quản trị trong Workbench.
2. File → Open SQL Script → `database/schema.sql` → chạy toàn bộ trên cơ sở dữ liệu mới.
3. Tạo tài khoản ứng dụng riêng bằng SQL sau; thay password trước khi chạy:

```sql
CREATE USER 'laptop_app'@'localhost' IDENTIFIED BY 'ThayBangMatKhauCuaBan@123';
GRANT SELECT, INSERT, UPDATE, DELETE ON laptop_store.* TO 'laptop_app'@'localhost';
```

4. Mở terminal PowerShell trong thư mục dự án:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/laptop_store?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8"
$env:DB_USER="laptop_app"
$env:DB_PASSWORD="ThayBangMatKhauCuaBan@123"
$env:APP_SEED="true"
$env:ADMIN_PASSWORD="MatKhauQuanTriRieng@123"
mvn spring-boot:run "-Dspring-boot.run.profiles=mysql"
```

5. `ddl-auto=validate`: ứng dụng kiểm tra schema, không tự tạo/xóa bảng MySQL. Nếu báo thiếu bảng: kiểm tra script và đúng DB_URL. Nếu báo Access denied: kiểm tra tài khoản/password/quyền và host của user.
6. Truy cập http://localhost:8080. MySQL và H2 là hai DB tách biệt; đăng ký tài khoản riêng trong môi trường đang dùng.
7. Sau lần seed đầu, có thể tắt `$env:APP_SEED="false"`.

`useSSL=false` dùng cho DB cục bộ. Khi triển khai server thật, cấu hình TLS cho DB và HTTPS cho website.

## 5. MySQL bằng Docker Compose (tùy chọn)

Chỉ dùng nếu Docker Engine đã chạy. Không cần sửa WSL để dùng cách MySQL trực tiếp ở mục 4.

```powershell
Copy-Item .env.example .env
```

Mở `.env`, thay các password mẫu. Sau đó:

```powershell
docker compose up -d mysql
docker compose ps
docker compose logs mysql
```

Cổng MySQL từ Windows là **3307**, cổng nội bộ container là 3306:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3307/laptop_store?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh"
$env:DB_USER="laptop_app"
$env:DB_PASSWORD="GiaTriDB_PASSWORDTrongFileEnv"
$env:APP_SEED="true"
$env:ADMIN_PASSWORD="MatKhauAdminCuaBan@123"
mvn spring-boot:run "-Dspring-boot.run.profiles=mysql"
```

**Maven không tự đọc `.env`.** `.env` được Docker Compose đọc; chạy Maven cần đặt biến `$env:...` như trên.

Chạy cả app trong Docker:

```powershell
docker compose --profile full up -d --build
```

Dừng mà giữ dữ liệu:

```powershell
docker compose --profile full down
```

Không dùng `down -v` nếu cần giữ DB. SQL init chỉ chạy khi volume MySQL còn trống; container chạy lại không tự import lại schema.

## 6. Các trang và luồng thao tác

| URL | Chức năng | Quyền |
|---|---|---|
| `/` | Tìm tên, lọc hãng/giá/nhu cầu, sắp xếp, phân trang | Công khai |
| `/products/{id}` | Cấu hình, ảnh và đánh giá | Công khai |
| `/register`, `/login` | Đăng ký và đăng nhập | Công khai |
| `/profile` | Họ tên, điện thoại, địa chỉ | Đăng nhập |
| `/cart`, `/checkout` | Giỏ hàng, địa chỉ, mã giảm, đặt hàng | Đăng nhập |
| `/orders`, `/orders/{id}` | Lịch sử và chi tiết đơn của mình | Chủ đơn |
| `/admin` | Doanh thu ngày/tháng/năm hoặc khoảng ngày | ADMIN |
| `/admin/products` | Thêm/sửa/ẩn laptop, cập nhật tồn, upload ảnh | ADMIN |
| `/admin/categories` | CRUD danh mục qua DAO/JDBC | ADMIN |
| `/admin/orders` | Duyệt, chuẩn bị, giao, hoàn tất | ADMIN |

Luồng demo:

1. Khách đăng ký → đăng nhập → tìm laptop → thêm giỏ → nhập địa chỉ → chọn COD.
2. Nhập `LAPTOP5` nếu muốn minh họa giảm 5%, tối đa 500.000 ₫, không có phí giao hàng.
3. Nhấn xác nhận → xem mã đơn. Tổng tiền lấy từ DB phía server.
4. Đăng nhập admin trong trình duyệt khác/cửa sổ riêng → Đơn hàng → chọn đơn.
5. Chuyển từng bước: `NEW` → `PREPARING` → `SHIPPING` → `DELIVERED`.
6. Với COD, thao tác đã giao cũng xác nhận đã thu tiền. Không dùng thao tác này nếu tiền chưa thu.
7. Khách quay lại trang sản phẩm → đánh giá 1–5 sao và bình luận.
8. Admin mở Dashboard → chọn Hôm nay để thấy doanh thu và sản phẩm bán chạy.

## 7. Đọc mã theo tầng

```text
Controller (Store/Admin/Api/Payment)
    → Service (Account/Catalog/Cart/Order/Review/Payment)
        → JPA Repository hoặc CategoryDao/JdbcCategoryDao/ReportDao
            → MySQL
```

- `OrderService` là interface, `OrderServiceImpl` chứa nghiệp vụ và `@Transactional`.
- `JdbcCategoryDao` dùng `JdbcTemplate`, SQL tham số hóa, có CRUD đầy đủ; UI danh mục gọi thật DAO này.
- `ReportDao` tổng hợp doanh thu bằng JDBC.
- Các repository còn lại dùng Spring Data JPA, ORM qua entity.
- `SecurityConfig` phân quyền ADMIN, session login, BCrypt, CSRF.
- `Forms` tách DTO nhập liệu khỏi entity để không cho khách tự gán role, giá đơn hay trạng thái thanh toán.
- Tất cả class đặt chung package `vn.laptop` cho bài học dễ mở; tầng được tách theo class/trách nhiệm. Dự án lớn nên chia subpackage và module.

Yêu cầu PDF nêu DAO/JDBC; yêu cầu mới của bạn nêu ORM/JPA. Bản này kết hợp hai cách và chứng minh CRUD JDBC qua danh mục. Nếu giảng viên yêu cầu **mọi bảng** đều CRUD bằng JDBC thì cần chuyển các Repository còn lại sang DAO JDBC; không nên khẳng định JPA tương đương yêu cầu JDBC tuyệt đối.

## 8. Test, build, export

```powershell
mvn clean test
mvn clean package
java -jar target/laptop-store-1.0.0.jar --spring.profiles.active=demo
```

Test dùng H2 in-memory và không kết nối DB cá nhân. Kết quả trong `target/surefire-reports`. Chi tiết kịch bản trong `docs/KIEM_THU.md`.

Xuất SQL từ MySQL sau khi chạy demo:

```powershell
.\scripts\export-db.ps1 -User laptop_app -Port 3306
```

Nếu dùng Docker port mapping, dùng `-Port 3307`. Cần `mysqldump` trong PATH và nhập password khi được hỏi. Script dùng `--result-file` để tránh PowerShell 5 xuất UTF-16.

Dùng Workbench thay thế: Server → Data Export → chọn `laptop_store` → Export to Self-Contained File → Start Export. Kiểm tra file có cả CREATE TABLE và INSERT; không nộp thông tin thanh toán/mật khẩu merchant.

`database/schema.sql` đi kèm là schema khởi tạo, **không phải bản export dữ liệu từ DB của bạn**. File export cần tự tạo sau demo để đáp ứng hồ sơ nộp.

## 9. Giới hạn thực tế

- Đây là dự án học tập, chưa phải cửa hàng production.
- Ảnh: một ảnh đại diện/laptop; cập nhật bằng upload JPEG/PNG. Chưa có album nhiều ảnh.
- Sản phẩm xóa mềm bằng `active=false` để bảo toàn đơn cũ. Danh mục chỉ xóa nếu chưa được dùng.
- Tồn kho là số lượng có thể bán; chưa có chứng từ nhập/xuất hoặc lịch sử điều chỉnh kho.
- Mã giảm giá demo cố định; chưa có màn hình quản lý chiến dịch.
- Thanh toán thật / hoàn tiền tự động / xuất hóa đơn / gửi email / xác minh email / quên mật khẩu chưa triển khai.
- Adapter VNPay/MoMo đã có tạo yêu cầu và kiểm chữ ký, kiểm tiền, chống IPN lặp; chưa xác nhận hoạt động đầu-cuối bằng merchant thật.
- Job hết hạn chạy khi app đang hoạt động; khi khởi động lại sẽ xử lý các đơn quá hạn.
- Không có SPA React/Angular trong bản này. Nếu chọn SPA thì thay Presentation Layer và bổ sung auth/API tương ứng.

## 10. Nguồn kỹ thuật

- Spring Boot: https://docs.spring.io/spring-boot/3.5/system-requirements.html
- VNPay PAY 2.1.0: https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html
- MoMo captureWallet: https://developers.momo.vn/v3/docs/payment/api/wallet/onetime/
- MoMo integration: https://developers.momo.vn/v3/vi/docs/payment/onboarding/integration-process/

## 11. Git trong VS Code

Sau khi kiểm tra hoạt động:

```powershell
git init
git add .
git status
git commit -m "Implement laptop store coursework"
```

Kiểm tra `git status` trước commit để chắc chắn không có `.env`, dữ liệu H2 hay ảnh upload cá nhân. Chỉ thêm remote và push sau khi nhóm đã tạo đúng repository của mình. Mỗi người commit phần thay đổi thực tế của họ.

Để đọc sơ đồ, mở `docs/uml/*.puml` bằng extension PlantUML trong VS Code và chọn Preview Current Diagram (thường Alt+D). Renderer local có thể cần Java và Graphviz; hoặc dùng renderer PlantUML do nhóm chọn. Các đặc tả và ERD dạng Mermaid đọc trong Markdown preview có hỗ trợ Mermaid.
