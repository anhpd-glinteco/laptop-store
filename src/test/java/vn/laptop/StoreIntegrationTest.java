package vn.laptop;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StoreIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired AccountRepository accounts;
  @Autowired ProductRepository products;
  @Autowired CartItemRepository carts;
  @Autowired ShopOrderRepository orders;
  @Autowired OrderItemRepository items;
  @Autowired ReviewRepository reviews;
  @Autowired CategoryDao categories;
  @Autowired OrderService service;
  @Autowired CartService cart;
  @Autowired ReviewService reviewService;
  @Autowired PasswordEncoder encoder;
  @Autowired PaymentGateway gateway;
  @Autowired ReportDao reports;
  Account buyer, other;
  Product laptop;

  @BeforeEach
  void setup() {
    reviews.deleteAll();
    items.deleteAll();
    orders.deleteAll();
    carts.deleteAll();
    products.deleteAll();
    accounts.deleteAll();
    for (Category c : categories.findAll()) categories.delete(c.id);
    categories.create("Laptop test");
    buyer = account("buyer@example.test");
    other = account("other@example.test");
    laptop = product("Laptop A", 10000000, 5);
    gateway.vnpCode = "TESTCODE";
    gateway.vnpSecret = "test-only-signing-secret";
    gateway.momoPartner = "TESTPARTNER";
    gateway.momoAccess = "test-access";
    gateway.momoSecret = "test-secret";
  }

  Account account(String email) {
    Account a = new Account();
    a.email = email;
    a.fullName = "Người thử nghiệm";
    a.passwordHash = encoder.encode("TestPassword@123");
    return accounts.save(a);
  }

  Product product(String name, long price, int stock) {
    Product p = new Product();
    p.name = name;
    p.brand = "Dell";
    p.purpose = "OFFICE";
    p.categoryId = categories.findAll().get(0).id;
    p.price = price;
    p.stock = stock;
    p.cpu = "i5";
    p.ram = "16 GB";
    p.storage = "512 GB";
    p.gpu = "Iris";
    p.screen = "FHD";
    return products.save(p);
  }

  Forms.Checkout form(String method) {
    Forms.Checkout f = new Forms.Checkout();
    f.fullName = "Nguyễn Văn A";
    f.phone = "0901234567";
    f.address = "123 Đường Mẫu, Hà Nội";
    f.paymentMethod = method;
    f.token = UUID.randomUUID().toString();
    return f;
  }

  ShopOrder order(String method) {
    cart.change(buyer.id, laptop.id, 1, true);
    return service.checkout(buyer.id, form(method));
  }

  @Test
  void checkoutSnapshotsPriceDiscountAndStock() {
    cart.change(buyer.id, laptop.id, 2, true);
    Forms.Checkout f = form("COD");
    f.coupon = "LAPTOP5";
    ShopOrder o = service.checkout(buyer.id, f);
    assertThat(o.subtotal).isEqualTo(20000000);
    assertThat(o.discount).isEqualTo(500000);
    assertThat(o.total).isEqualTo(19500000);
    assertThat(products.findById(laptop.id).orElseThrow().stock).isEqualTo(3);
    assertThat(carts.findByAccountIdOrderByProductIdAsc(buyer.id)).isEmpty();
    assertThat(items.findByOrderIdOrderByProductIdAsc(o.id).get(0).unitPrice).isEqualTo(10000000);
  }

  @Test
  void repeatedCheckoutIsIdempotent() {
    cart.change(buyer.id, laptop.id, 1, true);
    Forms.Checkout f = form("COD");
    ShopOrder first = service.checkout(buyer.id, f);
    assertThat(service.checkout(buyer.id, f).id).isEqualTo(first.id);
    assertThat(orders.count()).isEqualTo(1);
    assertThat(products.findById(laptop.id).orElseThrow().stock).isEqualTo(4);
  }

  @Test
  void insufficientStockRollsBackWholeOrder() {
    Product b = product("Laptop B", 20000000, 1);
    cart.change(buyer.id, laptop.id, 1, true);
    cart.change(buyer.id, b.id, 1, true);
    b.stock = 0;
    products.save(b);
    assertThatThrownBy(() -> service.checkout(buyer.id, form("COD")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(orders.count()).isZero();
    assertThat(products.findById(laptop.id).orElseThrow().stock).isEqualTo(5);
    assertThat(cart.lines(buyer.id)).hasSize(2);
  }

  @Test
  void twoCustomersCannotBuyTheLastUnitTwice() throws Exception {
    laptop.stock = 1;
    products.save(laptop);
    cart.change(buyer.id, laptop.id, 1, true);
    cart.change(other.id, laptop.id, 1, true);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch gate = new CountDownLatch(1);
    try {
      List<Future<Boolean>> fs = new ArrayList<>();
      for (Long id : List.of(buyer.id, other.id))
        fs.add(
            pool.submit(
                () -> {
                  gate.await();
                  try {
                    service.checkout(id, form("COD"));
                    return true;
                  } catch (IllegalArgumentException e) {
                    return false;
                  }
                }));
      gate.countDown();
      int wins = 0;
      for (var f : fs) if (f.get(15, TimeUnit.SECONDS)) wins++;
      assertThat(wins).isEqualTo(1);
      assertThat(products.findById(laptop.id).orElseThrow().stock).isZero();
      assertThat(orders.count()).isEqualTo(1);
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void otherUserCannotReadOrder() throws Exception {
    ShopOrder o = order("COD");
    mvc.perform(get("/orders/" + o.id).with(user(other.email))).andExpect(status().isForbidden());
  }

  @Test
  void csrfAndRoleAreEnforced() throws Exception {
    mvc.perform(get("/admin").with(user(buyer.email))).andExpect(status().isForbidden());
    mvc.perform(
            post("/cart/items")
                .with(user(buyer.email))
                .param("productId", laptop.id.toString())
                .param("quantity", "1"))
        .andExpect(status().isForbidden());
    mvc.perform(get("/admin")).andExpect(status().is3xxRedirection());
  }

  @Test
  void loginChecksHashedPassword() throws Exception {
    mvc.perform(formLogin().user(buyer.email).password("TestPassword@123"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/"));
    assertThat(buyer.passwordHash).startsWith("$2");
  }

  @Test
  void cancelRestoresStockOnlyOnce() {
    ShopOrder o = order("COD");
    service.cancel(buyer.id, o.id);
    assertThatThrownBy(() -> service.cancel(buyer.id, o.id))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(products.findById(laptop.id).orElseThrow().stock).isEqualTo(5);
  }

  @Test
  void statusCannotSkipAndReviewsRequireDelivery() {
    ShopOrder o = order("COD");
    assertThatThrownBy(() -> service.transition(o.id, "DELIVERED"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> reviewService.add(buyer.id, laptop.id, 5, "Tốt"))
        .isInstanceOf(IllegalArgumentException.class);
    service.transition(o.id, "PREPARING");
    service.transition(o.id, "SHIPPING");
    service.transition(o.id, "DELIVERED");
    reviewService.add(buyer.id, laptop.id, 5, "Máy tốt");
    assertThat(reviews.count()).isEqualTo(1);
    assertThatThrownBy(() -> reviewService.add(buyer.id, laptop.id, 4, "Lần hai"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> reviewService.add(other.id, laptop.id, 5, "Chưa mua"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(orders.findById(o.id).orElseThrow().paymentStatus).isEqualTo("PAID");
  }

  @Test
  void paymentRejectsWrongAmountAndProviderAndIsIdempotent() {
    ShopOrder o = order("VNPAY");
    assertThat(service.paymentResult(o.paymentRef, "VNPAY", 1, true, "txn")).isEqualTo("04");
    assertThat(service.paymentResult(o.paymentRef, "MOMO", o.total, true, "txn")).isEqualTo("04");
    assertThatThrownBy(() -> service.transition(o.id, "PREPARING"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(service.paymentResult(o.paymentRef, "VNPAY", o.total, true, "txn")).isEqualTo("00");
    assertThat(service.paymentResult(o.paymentRef, "VNPAY", o.total, true, "txn")).isEqualTo("02");
    assertThat(products.findById(laptop.id).orElseThrow().stock).isEqualTo(4);
  }

  @Test
  void expiryAndLatePaymentRequireReconciliation() {
    ShopOrder o = order("VNPAY");
    o.expiresAt = LocalDateTime.now().minusMinutes(1);
    orders.save(o);
    service.expire(o.id);
    service.expire(o.id);
    assertThat(products.findById(laptop.id).orElseThrow().stock).isEqualTo(5);
    assertThat(service.paymentResult(o.paymentRef, "VNPAY", o.total, true, "late")).isEqualTo("00");
    assertThat(orders.findById(o.id).orElseThrow().paymentStatus).isEqualTo("REVIEW_REQUIRED");
    assertThatThrownBy(() -> service.transition(o.id, "PREPARING"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void badVnpSignatureCannotMarkPaid() throws Exception {
    ShopOrder o = order("VNPAY");
    mvc.perform(
            get("/payments/vnpay/ipn")
                .param("vnp_TmnCode", "TESTCODE")
                .param("vnp_TxnRef", o.paymentRef)
                .param("vnp_Amount", String.valueOf(o.total * 100))
                .param("vnp_ResponseCode", "00")
                .param("vnp_TransactionStatus", "00")
                .param("vnp_SecureHash", "forged"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.RspCode").value("97"));
    assertThat(orders.findById(o.id).orElseThrow().paymentStatus).isEqualTo("PENDING");
  }

  @Test
  void signedVnpIpnUpdatesOnlyOnce() throws Exception {
    ShopOrder o = order("VNPAY");
    var d = new TreeMap<String, String>();
    d.put("vnp_TmnCode", gateway.vnpCode);
    d.put("vnp_TxnRef", o.paymentRef);
    d.put("vnp_Amount", String.valueOf(o.total * 100));
    d.put("vnp_ResponseCode", "00");
    d.put("vnp_TransactionStatus", "00");
    d.put("vnp_TransactionNo", "10001");
    d.put(
        "vnp_SecureHash",
        PaymentGateway.hmac("HmacSHA512", gateway.vnpSecret, PaymentGateway.vnpCanonical(d)));
    var req = get("/payments/vnpay/ipn");
    d.forEach(req::param);
    mvc.perform(req).andExpect(jsonPath("$.RspCode").value("00"));
    mvc.perform(req).andExpect(jsonPath("$.RspCode").value("02"));
  }

  @Test
  void unsignedMomoIsRejected() throws Exception {
    mvc.perform(
            post("/payments/momo/ipn")
                .contentType("application/json")
                .content("{\"orderId\":\"fake\",\"resultCode\":0}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void categoryJdbcCrudAndDeleteGuard() {
    categories.create("Phụ kiện");
    Category c =
        categories.findAll().stream()
            .filter(x -> x.name.equals("Phụ kiện"))
            .findFirst()
            .orElseThrow();
    categories.update(c.id, "Laptop cũ");
    assertThat(categories.findById(c.id).orElseThrow().name).isEqualTo("Laptop cũ");
    categories.delete(c.id);
    assertThat(categories.findById(c.id)).isEmpty();
    assertThatThrownBy(() -> categories.delete(laptop.categoryId))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void reportsExcludeCancelledAndOnlyCountCompleted() {
    ShopOrder cancelled = order("COD");
    service.cancel(buyer.id, cancelled.id);
    ShopOrder delivered = order("COD");
    service.transition(delivered.id, "PREPARING");
    service.transition(delivered.id, "SHIPPING");
    service.transition(delivered.id, "DELIVERED");
    Map<String, Object> s = reports.summary(LocalDate.now(), LocalDate.now());
    assertThat(((Number) s.get("revenue")).longValue()).isEqualTo(10000000);
    assertThat(((Number) s.get("order_count")).longValue()).isEqualTo(1);
    assertThat(reports.best(LocalDate.now(), LocalDate.now())).hasSize(1);
  }

  @Test
  void publicAndAuthenticatedTemplatesRender() throws Exception {
    for (String path :
        List.of("/", "/login", "/register", "/products/" + laptop.id, "/payments/return"))
      mvc.perform(get(path)).andExpect(status().isOk());
    cart.change(buyer.id, laptop.id, 1, true);
    for (String path : List.of("/cart", "/profile", "/checkout", "/orders"))
      mvc.perform(get(path).with(user(buyer.email))).andExpect(status().isOk());
    ShopOrder o = service.checkout(buyer.id, form("COD"));
    mvc.perform(get("/orders/" + o.id).with(user(buyer.email))).andExpect(status().isOk());
  }

  @Test
  void adminTemplatesRenderIncludingEditFields() throws Exception {
    ShopOrder o = order("COD");
    for (String path :
        List.of(
            "/admin",
            "/admin/products",
            "/admin/products/new",
            "/admin/products/" + laptop.id + "/edit",
            "/admin/categories",
            "/admin/orders",
            "/admin/orders/" + o.id))
      mvc.perform(get(path).with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
  }

  @Test
  void catalogFiltersAndSorts() throws Exception {
    Product second = product("Laptop B", 20000000, 1);
    mvc.perform(
            get("/api/products")
                .param("min", "15000000")
                .param("max", "25000000")
                .param("brand", "Dell")
                .param("purpose", "OFFICE")
                .param("sort", "priceAsc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(second.id))
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void validationRejectsBadCheckout() throws Exception {
    mvc.perform(
            post("/checkout")
                .with(user(buyer.email))
                .with(csrf())
                .param("fullName", "")
                .param("phone", "bad")
                .param("address", "x")
                .param("paymentMethod", "COD")
                .param("token", "a"))
        .andExpect(status().isOk())
        .andExpect(view().name("checkout"));
    assertThat(orders.count()).isZero();
  }

  @Autowired CatalogService catalog;

  @Test
  void staleAdminEditCannotOverwriteInventory() {
    Forms.ProductForm f = new Forms.ProductForm();
    org.springframework.beans.BeanUtils.copyProperties(laptop, f);
    f.stock = 50;
    order("COD");
    assertThatThrownBy(() -> catalog.save(f, null)).isInstanceOf(IllegalArgumentException.class);
    assertThat(products.findById(laptop.id).orElseThrow().stock).isEqualTo(4);
  }

  @Test
  void hiddenItemCanStillBeRemovedFromCart() {
    cart.change(buyer.id, laptop.id, 1, true);
    catalog.hide(laptop.id);
    cart.change(buyer.id, laptop.id, 0, false);
    assertThat(cart.lines(buyer.id)).isEmpty();
  }

  @Test
  void signedMomoIpnRecordsPaymentAndDuplicateIsSafe() throws Exception {
    ShopOrder o = order("MOMO");
    Map<String, Object> d = new LinkedHashMap<>();
    d.put("partnerCode", gateway.momoPartner);
    d.put("amount", o.total);
    d.put("extraData", "");
    d.put("message", "Successful.");
    d.put("orderId", o.paymentRef);
    d.put("orderInfo", "Thanh toan don " + o.id);
    d.put("orderType", "momo_wallet");
    d.put("payType", "qr");
    d.put("requestId", o.paymentRef);
    d.put("responseTime", 1730000000000L);
    d.put("resultCode", 0);
    d.put("transId", 123456L);
    String raw =
        "accessKey="
            + gateway.momoAccess
            + "&amount="
            + o.total
            + "&extraData=&message=Successful.&orderId="
            + o.paymentRef
            + "&orderInfo=Thanh toan don "
            + o.id
            + "&orderType=momo_wallet&partnerCode="
            + gateway.momoPartner
            + "&payType=qr&requestId="
            + o.paymentRef
            + "&responseTime=1730000000000&resultCode=0&transId=123456";
    d.put("signature", PaymentGateway.hmac("HmacSHA256", gateway.momoSecret, raw));
    String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(d);
    for (int n = 0; n < 2; n++)
      mvc.perform(post("/payments/momo/ipn").contentType("application/json").content(json))
          .andExpect(status().isNoContent());
    assertThat(orders.findById(o.id).orElseThrow().paymentStatus).isEqualTo("PAID");
    assertThat(products.findById(laptop.id).orElseThrow().stock).isEqualTo(4);
  }

  @Test
  void invalidCouponRollsBackStock() {
    cart.change(buyer.id, laptop.id, 1, true);
    Forms.Checkout f = form("COD");
    f.coupon = "INVALID";
    assertThatThrownBy(() -> service.checkout(buyer.id, f))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(products.findById(laptop.id).orElseThrow().stock).isEqualTo(5);
    assertThat(orders.count()).isZero();
  }
}
