# Đặc tả Use Case

Mẫu dưới gồm: tác nhân, mục tiêu, tiền điều kiện, kích hoạt, luồng chính, ngoại lệ, hậu điều kiện. Chưa có mẫu Word riêng của giảng viên, cần chuyển đúng mẫu trước khi nộp.

## UC01 — Đăng ký / đăng nhập

- Tác nhân: khách truy cập.
- Mục tiêu: tạo tài khoản CUSTOMER và truy cập chức năng mua hàng.
- Tiền điều kiện: chưa đăng nhập; hệ thống hoạt động.
- Kích hoạt: chọn Đăng ký hoặc Đăng nhập.
- Luồng chính: (1) Nhập họ tên, email, mật khẩu. (2) Server validate. (3) Chuẩn hóa email, kiểm tra duy nhất. (4) Băm mật khẩu BCrypt, gán CUSTOMER. (5) Chuyển trang login. (6) Nhập email/password. (7) Security xác thực, tạo session.
- Ngoại lệ: thiếu trường/độ dài không hợp lệ → trả form và lỗi; email trùng → không tạo; sai password → thông báo chung.
- Hậu điều kiện: một tài khoản được tạo hoặc một session hợp lệ; không lưu password plaintext.

## UC02 — Tra cứu laptop

- Tác nhân: khách hoặc khách đã đăng nhập.
- Mục tiêu: chọn sản phẩm theo tên, hãng, mức giá và mục đích.
- Tiền điều kiện: có danh mục sản phẩm active.
- Kích hoạt: tải trang chủ / nhấn Lọc.
- Luồng chính: (1) Nhập bộ lọc. (2) Server kiểm khoảng giá. (3) Query JPA Specification với AND. (4) Sắp theo whitelist. (5) Phân trang 12. (6) Mở chi tiết cấu hình.
- Ngoại lệ: không có kết quả → empty state; min > max → lỗi; sản phẩm đã ẩn → không cho mua.
- Hậu điều kiện: không thay đổi DB.

## UC03 — Đặt hàng COD

- Tác nhân: CUSTOMER.
- Mục tiêu: tạo đơn chính xác theo giỏ hàng, đảm bảo tồn kho.
- Tiền điều kiện: đã đăng nhập; giỏ có sản phẩm; thông tin giao hàng hợp lệ.
- Kích hoạt: nhấn Xác nhận đặt hàng.
- Luồng chính:
  1. Server kiểm CSRF, validate địa chỉ/điện thoại/phương thức/token.
  2. Khóa tài khoản để tránh giỏ thay đổi trong checkout.
  3. Kiểm tra token đã dùng; nếu đã dùng, trả lại đơn cũ của đúng người.
  4. Đọc giỏ từ DB; khóa các sản phẩm theo ID tăng dần.
  5. Kiểm active, số lượng và tồn; tính giá từ DB, không nhận giá từ client.
  6. Tính mã LAPTOP5 (nếu có), trừ stock, lưu đơn NEW/PENDING.
  7. Lưu OrderItem có tên/giá snapshot; xóa giỏ trong cùng transaction.
  8. Commit và hiển thị chi tiết đơn.
- Ngoại lệ:
  - E1: hết hàng ở bất kỳ dòng nào → rollback tất cả, không đơn dở dang.
  - E2: coupon không hợp lệ → rollback, giỏ giữ nguyên.
  - E3: bấm lại / gửi lại token → trả đơn đã tạo, không trừ kho thêm.
  - E4: token người khác → từ chối.
- Hậu điều kiện: đúng một đơn, tồn giảm đúng, giỏ trống; hoặc không đổi dữ liệu nếu thất bại.

## UC04 — Thanh toán online

- Tác nhân: CUSTOMER, cổng VNPay/MoMo.
- Mục tiêu: thanh toán đơn bằng sandbox và cập nhật có kiểm chứng.
- Tiền điều kiện: merchant đã cấu hình; đơn NEW/PENDING online còn hạn; public IPN dùng được.
- Kích hoạt: chọn thanh toán online và mở cổng.
- Luồng chính: (1) Tạo đơn, giữ kho 15 phút. (2) Khóa đơn, tạo paymentRef duy nhất. (3) Server ký request. (4) Redirect sang cổng. (5) Người dùng trả tiền thử nghiệm. (6) Cổng gọi IPN. (7) Xác minh chữ ký, merchant, ref, số tiền, provider. (8) Khóa đơn, cập nhật PAID một lần. (9) Khách xem lại chi tiết.
- Ngoại lệ:
  - Merchant trống → không cho đặt bằng cổng đó.
  - Cổng timeout → đơn vẫn PENDING; không tự đánh dấu thất bại hoặc paid.
  - Chữ ký giả / sai tiền → từ chối, không đổi đơn.
  - IPN lặp → trả acknowledged/already confirmed, không trừ kho thêm.
  - Thanh toán thất bại → hủy đơn và hoàn stock một lần.
  - Hết hạn → EXPIRED, hoàn stock; thành công đến muộn → REVIEW_REQUIRED, xử lý thủ công.
- Hậu điều kiện: chỉ PAID sau IPN hợp lệ. Return URL chỉ thông báo người dùng quay lại, không cập nhật paid.

## UC05 — Quản lý sản phẩm và kho

- Tác nhân: ADMIN.
- Tiền điều kiện: tài khoản ADMIN, danh mục tồn tại.
- Luồng chính: (1) Mở danh sách. (2) Thêm/sửa tên, hãng, cấu hình, giá, tồn, ảnh. (3) Server validate, kiểm quyền. (4) Khóa sản phẩm, kiểm version khi sửa. (5) Lưu và tải lại danh sách.
- Ngoại lệ: giá/tồn âm → lỗi; danh mục không tồn tại → lỗi; file không đúng JPEG/PNG hoặc quá lớn → lỗi; dữ liệu đã thay đổi từ lúc mở form → yêu cầu tải lại.
- Xóa sản phẩm: ẩn bằng active=false; đơn cũ vẫn có snapshot. Danh mục đang được dùng không thể xóa.
- Hậu điều kiện: catalog và kho khả dụng phản ánh giá trị mới.

## UC06 — Duyệt và giao đơn

- Tác nhân: ADMIN.
- Tiền điều kiện: đơn hợp lệ; online phải PAID trước duyệt.
- Luồng chính: (1) Đọc địa chỉ và hàng trong đơn. (2) NEW → PREPARING. (3) PREPARING → SHIPPING. (4) SHIPPING → DELIVERED. (5) COD ghi PAID, paidAt; mọi đơn ghi deliveredAt.
- Ngoại lệ: nhảy/trở ngược trạng thái → từ chối; online chưa trả tiền → không duyệt; đơn hết hạn/hủy → không giao.
- Hậu điều kiện: khách thấy trạng thái mới; doanh thu chỉ tính sau DELIVERED/PAID.

## UC07 — Đánh giá sau mua

- Tác nhân: CUSTOMER.
- Tiền điều kiện: đã mua đúng sản phẩm trong đơn DELIVERED; chưa đánh giá sản phẩm đó.
- Luồng chính: nhập 1–5 sao và bình luận → server kiểm chứng đơn → lưu gắn account/product → hiển thị bình luận được escape.
- Ngoại lệ: chưa nhận hàng/chưa mua, bình luận trống/quá dài, sao ngoài 1–5, đánh giá trùng → từ chối.
- Hậu điều kiện: mỗi người có tối đa một đánh giá/sản phẩm.

## UC08 — Xem báo cáo

- Tác nhân: ADMIN.
- Tiền điều kiện: đã đăng nhập admin.
- Luồng chính: chọn Hôm nay/Tháng này/Năm nay hoặc khoảng ngày → JDBC aggregate đơn DELIVERED/PAID theo deliveredAt → tổng tiền sau giảm, số đơn, doanh thu từng ngày, top 10 sản phẩm theo số lượng.
- Ngoại lệ: ngày kết thúc trước bắt đầu hoặc khoảng quá 5 năm → lỗi; không dữ liệu → tổng 0/empty state.
- Hậu điều kiện: không thay đổi dữ liệu.
