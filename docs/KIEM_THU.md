# Kế hoạch và kết quả kiểm thử

## Cách chạy

```powershell
mvn clean test
```

Bộ kiểm thử tích hợp dùng Spring Boot, MockMvc, JUnit 5 và H2 in-memory, profile `test`. Không thay đổi MySQL của bạn. Mockito dùng subclass mock maker để không cần quyền attach agent của JVM; các test hiện tại dùng service và database thật trong context, không giả lập nghiệp vụ bằng mock.

## Các trường hợp tự động

| # | Trường hợp | Kết quả cần đạt |
|---|---|---|
| 01 | Đặt COD + LAPTOP5 | Snapshot giá, giảm đúng, trừ kho, xóa giỏ |
| 02 | Gửi lại checkout token | Một đơn, không trừ kho lần hai |
| 03 | Một dòng giỏ thiếu hàng | Rollback cả đơn, các stock khác giữ nguyên |
| 04 | Hai khách mua chiếc cuối đồng thời | Chỉ một người thành công, stock không âm |
| 05 | Đọc đơn của tài khoản khác | HTTP 403 |
| 06 | Customer mở admin / POST thiếu CSRF | HTTP 403; anonymous admin chuyển login |
| 07 | Đăng nhập với BCrypt | Xác thực thành công, hash không là plaintext |
| 08 | Hủy COD hai lần | Chỉ hoàn stock lần đầu |
| 09 | Nhảy trạng thái / đánh giá trước và sau giao | Chặn nhảy, chỉ đánh giá sau giao, chống trùng |
| 10 | Callback sai amount/provider và lặp | Từ chối sai tiền, đúng chỉ áp dụng một lần |
| 11 | Online hết hạn, callback thành công đến muộn | Hoàn kho một lần; REVIEW_REQUIRED |
| 12 | VNPay chữ ký giả | RspCode 97, đơn vẫn pending |
| 13 | VNPay chữ ký hợp lệ, gửi lặp | Lần đầu 00, lần sau 02 |
| 14 | MoMo thiếu chữ ký | HTTP 400 |
| 15 | JDBC CRUD danh mục | Đọc/thêm/sửa/xóa; chặn xóa danh mục đang dùng |
| 16 | Báo cáo có đơn hủy và đơn hoàn tất | Chỉ cộng đơn DELIVERED/PAID |
| 17 | Render các trang công khai/khách | HTTP 200 và template không lỗi |
| 18 | Render tất cả trang admin, form sửa | HTTP 200 và template không lỗi |
| 19 | API lọc hãng + khoảng giá + mục đích | Trả đúng sản phẩm |
| 20 | Checkout địa chỉ/điện thoại không hợp lệ | Trả form lỗi, không tạo đơn |
| 21 | Admin lưu form kho cũ sau khi khách mua | Từ chối, không ghi đè tồn mới |
| 22 | Sản phẩm trong giỏ bị ẩn | Vẫn xóa khỏi giỏ được |
| 23 | MoMo IPN đã ký, gửi hai lần | Cả hai acknowledged; PAID, không trừ kho lần hai |
| 24 | Mã giảm không tồn tại | Rollback stock, không có đơn |

`docs/TEST_RESULT.txt` ghi kết quả build/test thực tế của bản bàn giao. Bộ test dùng chữ ký và secret dành riêng cho test, không chứng minh đã kết nối merchant thật.

## Hai test minh họa nên trình bày

**TC01 — đặt hàng hợp lệ**

- Chuẩn bị: laptop giá 10.000.000 ₫, tồn 5; giỏ 2 máy; coupon LAPTOP5.
- Thực hiện: OrderService.checkout.
- Mong đợi: tạm tính 20.000.000 ₫; giảm 500.000 ₫; thanh toán 19.500.000 ₫; tồn 3; 1 đơn; giỏ trống.
- Mã: `checkoutSnapshotsPriceDiscountAndStock`.

**TC02 — hết hàng giữa lúc mua**

- Chuẩn bị: giỏ có A và B; A còn 5; B hết hàng sau khi đã thêm giỏ.
- Thực hiện: checkout.
- Mong đợi: báo hết hàng; không đơn; tồn A vẫn 5; giỏ không mất.
- Mã: `insufficientStockRollsBackWholeOrder`.

## Postman

1. Import `postman/LaptopStore.postman_collection.json`.
2. Chạy server, đặt collection variable `baseUrl` đúng cổng.
3. Dùng email mới ở lần đăng ký đầu tiên; không dùng tài khoản admin cho flow customer.
4. Giữ cookie jar; không xóa JSESSIONID giữa các request.
5. Lấy `/api/csrf` trước POST. Sau đăng nhập lấy lại token vì session/CSRF có thể thay đổi.
6. Chạy tuần tự 01–15. Tắt Automatically follow redirects để thấy HTTP 302; nếu bật, có thể nhận HTML cuối cùng 200.
7. Collection lấy productId, checkoutToken, orderId từ kết quả trước.
8. Đọc đơn trước và sau request 11 để xác nhận không tăng số đơn do token cũ.

Đây là mô hình MVC session: nhiều endpoint POST trả redirect HTML, không phải mọi endpoint đều trả JSON REST. JSON công khai: `/api/products`; JSON sau login: `/api/orders`; `/api/csrf` cấp token. Không thêm Basic Auth hoặc JWT vào collection này.

## Kiểm tra thủ công trước nộp

| Môi trường / chức năng | Các bước |
|---|---|
| MySQL thật | Import schema, khởi động mysql profile với validate, tạo đơn, kiểm SELECT stock/order/items |
| MySQL đồng thời | Hai tài khoản cùng checkout sản phẩm stock=1; xác nhận một đơn và không stock âm |
| Giao diện | Mở Chrome ở 1440px và 390px; kiểm menu, bảng cuộn, form, ảnh, lỗi nhập liệu |
| Ảnh | Upload PNG/JPEG thật; thử SVG, ảnh quá 2 MB, ảnh quá 4000×4000 |
| Danh mục | Tạo mới, sửa, xóa trống; thử xóa danh mục có sản phẩm |
| Phân quyền | URL admin với customer; URL đơn khác; logout xong thử truy cập lại |
| XSS | Bình luận chứa thẻ HTML hiển thị dưới dạng chữ, không thực thi |
| Sandbox merchant | IPN chữ ký thật, thành công, thất bại, lặp, sai amount, đến muộn |
| SQL export | Import bản export vào một DB thử riêng và kiểm tra đủ bảng/dữ liệu |
| Docker | Chạy `docker compose config`, MySQL healthy và app full profile |

## Phạm vi đã và chưa xác minh

- Đã kiểm tra bằng bộ integration test H2/MockMvc: các trường hợp có trong TEST_RESULT.
- Việc MockMvc render template chứng minh template xử lý được; không thay thế kiểm tra bố cục bằng trình duyệt.
- Chưa chạy MySQL Server/Docker hoặc merchant sandbox trong môi trường tạo gói này.
- Chưa kiểm tải/production, hoàn tiền hay kết nối vận chuyển.
- Mã PlantUML là nguồn sơ đồ để nhóm render; chưa có ảnh sơ đồ được render tự động trong gói.
