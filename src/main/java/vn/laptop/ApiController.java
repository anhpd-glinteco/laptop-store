package vn.laptop;

import java.security.Principal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ApiController {
  private final CatalogService catalog;
  private final ShopOrderRepository orders;
  private final AccountService accounts;

  public ApiController(CatalogService c, ShopOrderRepository o, AccountService a) {
    catalog = c;
    orders = o;
    accounts = a;
  }

  public record ProductView(
      Long id,
      String name,
      String brand,
      String purpose,
      long price,
      int stock,
      String cpu,
      String ram,
      String storage,
      String gpu,
      String screen,
      String imageUrl) {}

  @GetMapping("/products")
  public java.util.List<ProductView> products(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "") String brand,
      @RequestParam(defaultValue = "") String purpose,
      @RequestParam(required = false) Long min,
      @RequestParam(required = false) Long max,
      @RequestParam(defaultValue = "newest") String sort,
      @RequestParam(defaultValue = "0") int page) {
    return catalog
        .search(q, brand, purpose, min, max, sort, page)
        .map(
            p ->
                new ProductView(
                    p.id,
                    p.name,
                    p.brand,
                    p.purpose,
                    p.price,
                    p.stock,
                    p.cpu,
                    p.ram,
                    p.storage,
                    p.gpu,
                    p.screen,
                    p.imageUrl))
        .getContent();
  }

  @GetMapping("/csrf")
  public java.util.Map<String, String> csrf(CsrfToken t) {
    return java.util.Map.of(
        "headerName",
        t.getHeaderName(),
        "parameterName",
        t.getParameterName(),
        "token",
        t.getToken());
  }

  @GetMapping("/orders")
  public java.util.List<java.util.Map<String, Object>> orders(Principal p) {
    return orders.findByAccountIdOrderByCreatedAtDesc(accounts.current(p.getName()).id).stream()
        .map(
            o ->
                java.util.Map.<String, Object>of(
                    "id",
                    o.id,
                    "status",
                    o.status,
                    "paymentStatus",
                    o.paymentStatus,
                    "total",
                    o.total))
        .toList();
  }
}
