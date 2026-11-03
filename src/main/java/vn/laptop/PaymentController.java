package vn.laptop;

import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {
  private final PaymentGateway gateway;
  private final OrderService orders;

  public PaymentController(PaymentGateway g, OrderService o) {
    gateway = g;
    orders = o;
  }

  @GetMapping("/vnpay/ipn")
  public Map<String, String> vnp(@RequestParam Map<String, String> d) {
    if (!gateway.verifyVnp(d)) return answer("97");
    try {
      long raw = Long.parseLong(d.get("vnp_Amount"));
      if (raw <= 0 || raw % 100 != 0) return answer("04");
      String result =
          orders.paymentResult(
              d.get("vnp_TxnRef"),
              "VNPAY",
              raw / 100,
              "00".equals(d.get("vnp_ResponseCode")) && "00".equals(d.get("vnp_TransactionStatus")),
              d.get("vnp_TransactionNo"));
      return answer(result);
    } catch (Exception e) {
      return answer("99");
    }
  }

  private Map<String, String> answer(String code) {
    return Map.of(
        "RspCode",
        code,
        "Message",
        switch (code) {
          case "00" -> "Confirm Success";
          case "02" -> "Already confirmed";
          case "01" -> "Order not found";
          case "04" -> "Invalid amount or provider";
          case "97" -> "Invalid signature";
          default -> "Unknown error";
        });
  }

  @PostMapping("/momo/ipn")
  public ResponseEntity<Void> momo(@RequestBody Map<String, Object> d) {
    if (!gateway.verifyMomo(d)) return ResponseEntity.badRequest().build();
    try {
      String ref = String.valueOf(d.get("orderId"));
      if (!ref.equals(String.valueOf(d.get("requestId"))))
        return ResponseEntity.badRequest().build();
      String code =
          orders.paymentResult(
              ref,
              "MOMO",
              Long.parseLong(String.valueOf(d.get("amount"))),
              "0".equals(String.valueOf(d.get("resultCode"))),
              String.valueOf(d.get("transId")));
      return Set.of("00", "02").contains(code)
          ? ResponseEntity.noContent().build()
          : ResponseEntity.badRequest().build();
    } catch (Exception e) {
      return ResponseEntity.internalServerError().build();
    }
  }
}
