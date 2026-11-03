package vn.laptop;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class PaymentGateway {
  @Value("${payment.vnpay.code}")
  String vnpCode;

  @Value("${payment.vnpay.secret}")
  String vnpSecret;

  @Value("${payment.momo.partner}")
  String momoPartner;

  @Value("${payment.momo.access}")
  String momoAccess;

  @Value("${payment.momo.secret}")
  String momoSecret;

  @Value("${app.base-url}")
  String base;

  public boolean enabled(String type) {
    return switch (type) {
      case "COD" -> true;
      case "VNPAY" -> !vnpCode.isBlank() && !vnpSecret.isBlank();
      case "MOMO" -> !momoPartner.isBlank() && !momoAccess.isBlank() && !momoSecret.isBlank();
      default -> false;
    };
  }

  public static String hmac(String alg, String key, String raw) {
    try {
      Mac m = Mac.getInstance(alg);
      m.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), alg));
      return HexFormat.of().formatHex(m.doFinal(raw.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException("Không thể ký dữ liệu", e);
    }
  }

  public static boolean equal(String a, String b) {
    return a != null
        && b != null
        && MessageDigest.isEqual(
            a.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII),
            b.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
  }

  static String enc(String s) {
    return URLEncoder.encode(s, StandardCharsets.UTF_8);
  }

  public static String vnpCanonical(Map<String, String> data) {
    return new TreeMap<>(data)
        .entrySet().stream()
            .filter(
                e ->
                    e.getKey().startsWith("vnp_")
                        && !Set.of("vnp_SecureHash", "vnp_SecureHashType").contains(e.getKey())
                        && e.getValue() != null
                        && !e.getValue().isEmpty())
            .map(e -> enc(e.getKey()) + "=" + enc(e.getValue()))
            .collect(java.util.stream.Collectors.joining("&"));
  }

  public boolean verifyVnp(Map<String, String> data) {
    return enabled("VNPAY")
        && vnpCode.equals(data.get("vnp_TmnCode"))
        && equal(hmac("HmacSHA512", vnpSecret, vnpCanonical(data)), data.get("vnp_SecureHash"));
  }

  public String url(ShopOrder o, String ip) {
    if (!enabled(o.paymentMethod))
      throw new IllegalArgumentException("Chưa cấu hình cổng thanh toán");
    return o.paymentMethod.equals("VNPAY") ? vnpUrl(o, ip) : momoUrl(o);
  }

  private String vnpUrl(ShopOrder o, String ip) {
    var d = new TreeMap<String, String>();
    var fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    d.put("vnp_Version", "2.1.0");
    d.put("vnp_Command", "pay");
    d.put("vnp_TmnCode", vnpCode);
    d.put("vnp_Amount", String.valueOf(Math.multiplyExact(o.total, 100)));
    d.put("vnp_CurrCode", "VND");
    d.put("vnp_TxnRef", o.paymentRef);
    d.put("vnp_OrderInfo", "Thanh toan don " + o.id);
    d.put("vnp_OrderType", "other");
    d.put("vnp_Locale", "vn");
    d.put("vnp_ReturnUrl", base + "/payments/return");
    d.put("vnp_IpAddr", ip);
    d.put(
        "vnp_CreateDate",
        o.createdAt
            .atZone(ZoneId.systemDefault())
            .withZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh"))
            .format(fmt));
    d.put(
        "vnp_ExpireDate",
        o.expiresAt
            .atZone(ZoneId.systemDefault())
            .withZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh"))
            .format(fmt));
    String q = vnpCanonical(d);
    return "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?"
        + q
        + "&vnp_SecureHash="
        + hmac("HmacSHA512", vnpSecret, q);
  }

  private String momoUrl(ShopOrder o) {
    var d = new LinkedHashMap<String, Object>();
    d.put("partnerCode", momoPartner);
    d.put("requestId", o.paymentRef);
    d.put("amount", o.total);
    d.put("orderId", o.paymentRef);
    d.put("orderInfo", "Thanh toan don " + o.id);
    d.put("redirectUrl", base + "/payments/return");
    d.put("ipnUrl", base + "/payments/momo/ipn");
    d.put("lang", "vi");
    d.put("requestType", "captureWallet");
    d.put("autoCapture", true);
    d.put("extraData", "");
    String raw =
        "accessKey="
            + momoAccess
            + "&amount="
            + o.total
            + "&extraData=&ipnUrl="
            + d.get("ipnUrl")
            + "&orderId="
            + o.paymentRef
            + "&orderInfo="
            + d.get("orderInfo")
            + "&partnerCode="
            + momoPartner
            + "&redirectUrl="
            + d.get("redirectUrl")
            + "&requestId="
            + o.paymentRef
            + "&requestType=captureWallet";
    d.put("signature", hmac("HmacSHA256", momoSecret, raw));
    var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(5000);
    factory.setReadTimeout(10000);
    try {
      Map<?, ?> result =
          RestClient.builder()
              .requestFactory(factory)
              .build()
              .post()
              .uri("https://test-payment.momo.vn/v2/gateway/api/create")
              .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
              .body(d)
              .retrieve()
              .body(Map.class);
      if (result == null || !"0".equals(String.valueOf(result.get("resultCode"))))
        throw new IllegalArgumentException(
            "MoMo chưa tạo được phiên thanh toán. Kiểm tra tài khoản sandbox / đối soát requestId"
                + " trước khi thử lại");
      String url = String.valueOf(result.get("payUrl"));
      URI u = URI.create(url);
      if (!"https".equals(u.getScheme()) || !"test-payment.momo.vn".equals(u.getHost()))
        throw new IllegalArgumentException("Địa chỉ MoMo không hợp lệ");
      return url;
    } catch (org.springframework.web.client.RestClientException e) {
      throw new IllegalArgumentException(
          "Không kết nối được MoMo. Đơn vẫn chờ; cần đối soát nếu yêu cầu đã gửi tới cổng");
    }
  }

  public boolean verifyMomo(Map<String, Object> d) {
    if (!enabled("MOMO") || !momoPartner.equals(String.valueOf(d.get("partnerCode")))) return false;
    StringBuilder raw = new StringBuilder("accessKey=" + momoAccess);
    for (String key :
        List.of(
            "amount",
            "extraData",
            "message",
            "orderId",
            "orderInfo",
            "orderType",
            "partnerCode",
            "payType",
            "requestId",
            "responseTime",
            "resultCode",
            "transId")) {
      if (!d.containsKey(key) || d.get(key) == null) return false;
      raw.append('&').append(key).append('=').append(d.get(key));
    }
    return equal(
        hmac("HmacSHA256", momoSecret, raw.toString()), String.valueOf(d.get("signature")));
  }
}
