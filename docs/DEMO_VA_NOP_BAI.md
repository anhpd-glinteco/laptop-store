# Kịch bản demo và hồ sơ TX1

## Kế hoạch 20 phút theo đề

| Thời lượng | Nội dung |
|---|---|
| 0–10 phút | Trình bày: bài toán, tác nhân, UML, kiến trúc, CSDL, các chức năng, bảo mật |
| 10–15 phút | Vấn đáp cá nhân: giải thích phần mình làm, transaction, JDBC/JPA, truy vấn |
| 15–20 phút | Live demo: MySQL, CRUD, đặt COD, cập nhật giao hàng, test |

Mở app, MySQL và terminal test sẵn trước lượt nhóm. Mỗi thành viên trình bày phần tự làm/hiểu được, không gán đóng góp không có thật.

## Slide gợi ý (8 trang)

1. Tên đề tài, nhóm/thành viên, phân công thực tế.
2. Báo cáo & Thiết kế: yêu cầu, Use Case tổng quát.
3. Báo cáo & Thiết kế: ERD và Sequence đặt hàng.
4. Source Code & Demo: tầng controller/service/repository/DAO; JDBC CRUD danh mục.
5. Source Code & Demo: đặt hàng an toàn, screenshot UI do nhóm chụp khi chạy.
6. Source Code & Demo: các test, kết quả chạy và giới hạn sandbox.
7. Kỹ năng thuyết trình: phân bổ thời lượng và người trình bày; có thể thể hiện qua phần phân công.
8. Vấn đáp cá nhân: câu hỏi dự kiến, đóng góp và hướng phát triển.

Slide nộp cần có 4 phần tương ứng rubric. Gói mã này chưa chứa PPTX/Word theo mẫu trường.

## Live demo 5 phút

- 00:00–00:35: Mở Workbench, chạy `SELECT id,name,stock FROM products;` để chứng minh kết nối dữ liệu thật.
- 00:35–01:15: Admin tạo danh mục mới rồi sửa tên; chỉ file JdbcCategoryDao có SQL INSERT/UPDATE/DELETE để giải thích JDBC.
- 01:15–02:20: Khách chọn máy, checkout COD; hiển thị tổng giảm và mã đơn, so sánh stock trong DB.
- 02:20–03:15: Admin duyệt → đang giao → đã giao; khách xem lịch sử và đánh giá.
- 03:15–04:00: Dashboard doanh thu Hôm nay; truy vấn dữ liệu.
- 04:00–05:00: Chạy `mvn test`, mở test checkout và rollback hết hàng để giải thích.

Không chọn demo thanh toán online nếu chưa có merchant/IPN hoạt động. Nói rõ COD đã chạy, online là mã tích hợp sandbox chưa xác minh với cổng.

## Câu hỏi vấn đáp

1. Vì sao không lấy giá từ request? Vì client có thể sửa giá; lấy DB và snapshot lúc tạo đơn.
2. Transaction giải quyết gì? Nếu một sản phẩm hết hàng, rollback đơn, chi tiết, stock và giỏ.
3. Hai khách cùng mua chiếc cuối? PESSIMISTIC_WRITE khóa bản ghi; một người thành công, người sau đọc tồn mới và bị từ chối.
4. Vì sao lưu unit_price trong order_items? Giá catalog có thể thay đổi; đơn cũ giữ giá tại thời điểm mua.
5. JPA và JDBC dùng chỗ nào? Repository ở miền giao dịch; JdbcCategoryDao CRUD và ReportDao aggregate.
6. CSRF là gì? Token gắn session chống website khác gửi POST thay người dùng; IPN MoMo dùng chữ ký thay token trình duyệt.
7. Return URL có đủ để ghi paid? Không. Chỉ IPN đã xác minh chữ ký và số tiền.
8. Vì sao không xóa vật lý laptop? Để bảo toàn liên kết và lịch sử đơn; chỉ ngừng hiển thị.
9. Doanh thu tính lúc nào? Khi DELIVERED/PAID theo ngày giao, tổng tiền sau giảm; không đếm đơn hủy.
10. Điểm chưa làm? Hoàn tiền/query tự động, album ảnh, audit kho, production hardening, Word/PPTX theo mẫu.

## Hồ sơ nộp

- [ ] Chạy thành công bằng MySQL và test trên máy nhóm.
- [ ] Chụp minh chứng UI/SQL/test thật; không dùng kết quả chưa chạy.
- [ ] Export `database/laptop_store_export.sql` có schema + dữ liệu mẫu phù hợp.
- [ ] Đổi tên ZIP `SourceCode_Nhom[XX].zip`, gồm source và SQL export.
- [ ] Tạo `BaoCao_Nhom[XX].docx` theo mẫu BTL của giảng viên, dùng nội dung trong docs làm cơ sở.
- [ ] Tạo `Slide_Nhom[XX].pptx` theo 4 phần rubric.
- [ ] Kiểm tra ZIP không chứa `.env`, DB cá nhân, merchant secret, password thật hoặc target cache.
- [ ] Tạo repository GitHub/GitLab nếu nhóm muốn; không tự tạo lịch sử commit giả.
- [ ] Kiểm tra người nhận/quyền Drive và nộp trước **18:00 ngày 04/11/2026 (giờ Việt Nam)** theo PDF đã cung cấp.

Không có thao tác gửi/nộp tự động trong dự án này.
