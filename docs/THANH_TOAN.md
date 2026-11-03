# Tích hợp thanh toán sandbox

## Trạng thái bàn giao

Có mã tạo URL VNPay, gọi MoMo captureWallet, kiểm chữ ký IPN, kiểm merchant/số tiền/ref, khóa đơn và xử lý callback lặp. **Chưa có thông tin merchant, chưa chạy giao dịch sandbox đầu-cuối.** Không mô tả phần này là đã được cổng chứng nhận. COD dùng được độc lập.

Chỉ dùng thông tin sandbox do VNPay/MoMo cấp. Không dùng dữ liệu thẻ thật. Không gửi secret vào Git, Postman collection hay báo cáo.

## VNPay

Biến PowerShell:

```powershell
$env:VNPAY_TMN_CODE="MaMerchantSandbox"
$env:VNPAY_HASH_SECRET="SecretSandboxCuaBan"
$env:APP_BASE_URL="https://ten-mien-thu-nghiem-cua-ban"
```

Đăng ký IPN với VNPay: `https://ten-mien-thu-nghiem-cua-ban/payments/vnpay/ipn`.
Return URL: `/payments/return`. Cổng sandbox: `https://sandbox.vnpayment.vn/paymentv2/vpcpay.html`.

Request v2.1.0 gồm merchant, TxnRef, số tiền ×100, currency VND, thời điểm Asia/Ho_Chi_Minh, thời hạn, IP thực của request. Giá trị được URL-encode UTF-8 rồi sắp key trước HMAC-SHA512. Không tin header X-Forwarded-For tùy ý từ client; khi qua reverse proxy cần cấu hình trusted proxy cụ thể.

IPN: kiểm checksum trước → kiểm merchant/ref/provider → kiểm số tiền chia 100 không dư → yêu cầu ResponseCode=00 và TransactionStatus=00 để paid. Mã đáp ứng 00 thành công, 02 đã xử lý, 01 không có đơn, 04 sai tiền/provider, 97 sai chữ ký, 99 lỗi xử lý.

## MoMo

```powershell
$env:MOMO_PARTNER_CODE="PartnerCodeSandbox"
$env:MOMO_ACCESS_KEY="AccessKeySandbox"
$env:MOMO_SECRET_KEY="SecretKeySandbox"
$env:APP_BASE_URL="https://ten-mien-thu-nghiem-cua-ban"
```

Tạo request đến `https://test-payment.momo.vn/v2/gateway/api/create`, requestType `captureWallet`, autoCapture=true, amount là VND nguyên. RequestId=orderId=paymentRef, duy nhất theo đơn. IPN POST tới `/payments/momo/ipn`.

Chữ ký HMAC-SHA256 cho create theo chuỗi khóa do MoMo quy định. Callback dùng tập khóa accessKey, amount, extraData, message, orderId, orderInfo, orderType, partnerCode, payType, requestId, responseTime, resultCode, transId. Chỉ resultCode=0 được xem là paid; kiểm cả orderId=requestId và tổng tiền.

Timeout tạo giao dịch không chứng minh giao dịch chưa được tạo. Không phát sinh requestId mới để thử lại mù quáng: giữ cùng ref, xem portal merchant để đối soát. Chức năng query/refund tự động chưa có trong dự án này.

## Trình tự kiểm tra bằng merchant

1. Chạy MySQL, app và public HTTPS có thể nhận request từ cổng. `localhost` không đủ cho IPN server-to-server.
2. Đặt biến môi trường, khởi động lại app. Các lựa chọn online được bật.
3. Đăng nhập customer, thêm giỏ, tạo đơn bằng provider muốn kiểm tra.
4. Mở cổng; dùng tài khoản/thẻ test do cổng cung cấp.
5. Kiểm IPN được cổng gọi và đơn PAID; return URL riêng không được làm paid.
6. Gửi lại cùng IPN đã ký → không trừ stock lại, không tạo đơn mới.
7. Sửa amount hoặc signature → từ chối.
8. Thử hủy/thất bại, hết hạn và callback đến muộn.
9. Ghi lại kết quả và đối chiếu portal merchant trước demo thanh toán.

Thông tin portal, giới hạn amount, phương thức thử nghiệm và quy trình onboarding có thể khác theo hợp đồng; kiểm tra lại tài liệu merchant thực tế. Dự án cố định endpoint sandbox, không có công tắc production để tránh chuyển nhầm.

Nguồn:
- https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html
- https://developers.momo.vn/v3/docs/payment/api/wallet/onetime/
- https://developers.momo.vn/v3/vi/docs/payment/onboarding/integration-process/
