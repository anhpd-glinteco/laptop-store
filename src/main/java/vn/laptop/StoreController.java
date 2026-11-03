package vn.laptop;

import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class StoreController {
  private final CatalogService catalog;
  private final AccountService accounts;
  private final CartService cart;
  private final OrderService orders;
  private final ShopOrderRepository orderRepo;
  private final OrderItemRepository items;
  private final ReviewRepository reviews;
  private final ReviewService reviewService;
  private final PaymentGateway gateway;
  private final PaymentService payments;

  public StoreController(
      CatalogService c,
      AccountService a,
      CartService ca,
      OrderService o,
      ShopOrderRepository or,
      OrderItemRepository i,
      ReviewRepository r,
      ReviewService rs,
      PaymentGateway g,
      PaymentService p) {
    catalog = c;
    accounts = a;
    cart = ca;
    orders = o;
    orderRepo = or;
    items = i;
    reviews = r;
    reviewService = rs;
    gateway = g;
    payments = p;
  }

  long uid(Principal p) {
    return accounts.current(p.getName()).id;
  }

  @GetMapping("/")
  String home(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "") String brand,
      @RequestParam(defaultValue = "") String purpose,
      @RequestParam(required = false) Long min,
      @RequestParam(required = false) Long max,
      @RequestParam(defaultValue = "newest") String sort,
      @RequestParam(defaultValue = "0") int page,
      Model m) {
    m.addAttribute("brands", catalog.brands());
    m.addAttribute("page", catalog.search(q, brand, purpose, min, max, sort, page));
    m.addAttribute("q", q);
    m.addAttribute("brand", brand);
    m.addAttribute("purpose", purpose);
    m.addAttribute("min", min);
    m.addAttribute("max", max);
    m.addAttribute("sort", sort);
    return "home";
  }

  @GetMapping("/products/{id}")
  String product(@PathVariable long id, Model m) {
    Product p = catalog.get(id);
    if (!p.active) throw new IllegalArgumentException("Sản phẩm đã ngừng bán");
    m.addAttribute("product", p);
    m.addAttribute("reviews", reviews.findByProductIdOrderByCreatedAtDesc(id));
    return "product";
  }

  @GetMapping("/login")
  String login() {
    return "login";
  }

  @GetMapping("/register")
  String register(Model m) {
    m.addAttribute("form", new Forms.Register());
    return "register";
  }

  @PostMapping("/register")
  String register(
      @Valid @ModelAttribute("form") Forms.Register f,
      BindingResult result,
      RedirectAttributes ra) {
    if (result.hasErrors()) return "register";
    try {
      accounts.register(f);
    } catch (IllegalArgumentException | org.springframework.dao.DataIntegrityViolationException e) {
      result.rejectValue("email", "duplicate", "Email đã tồn tại hoặc không hợp lệ");
      return "register";
    }
    ra.addFlashAttribute("message", "Đăng ký thành công. Hãy đăng nhập");
    return "redirect:/login";
  }

  @GetMapping("/profile")
  String profile(Principal p, Model m) {
    Account a = accounts.current(p.getName());
    Forms.Profile f = new Forms.Profile();
    f.fullName = a.fullName;
    f.phone = a.phone;
    f.address = a.address;
    m.addAttribute("form", f);
    return "profile";
  }

  @PostMapping("/profile")
  String profile(
      Principal p,
      @Valid @ModelAttribute("form") Forms.Profile f,
      BindingResult r,
      RedirectAttributes ra) {
    if (r.hasErrors()) return "profile";
    accounts.profile(uid(p), f);
    ra.addFlashAttribute("message", "Đã lưu hồ sơ");
    return "redirect:/profile";
  }

  @GetMapping("/cart")
  String cart(Principal p, Model m) {
    m.addAttribute("lines", cart.lines(uid(p)));
    m.addAttribute("total", cart.total(uid(p)));
    return "cart";
  }

  @PostMapping("/cart/items")
  String add(
      Principal p, @RequestParam long productId, @RequestParam(defaultValue = "1") int quantity) {
    cart.change(uid(p), productId, quantity, true);
    return "redirect:/cart";
  }

  @PostMapping("/cart/update")
  String update(Principal p, @RequestParam long productId, @RequestParam int quantity) {
    cart.change(uid(p), productId, quantity, false);
    return "redirect:/cart";
  }

  void checkoutModel(Principal p, Model m) {
    m.addAttribute("total", cart.total(uid(p)));
    m.addAttribute("vnpay", gateway.enabled("VNPAY"));
    m.addAttribute("momo", gateway.enabled("MOMO"));
  }

  @GetMapping("/checkout")
  String checkout(Principal p, Model m, HttpSession session) {
    Account a = accounts.current(p.getName());
    Forms.Checkout f = new Forms.Checkout();
    f.fullName = a.fullName;
    f.phone = a.phone;
    f.address = a.address;
    String token = (String) session.getAttribute("checkoutToken");
    if (token == null) {
      token = UUID.randomUUID().toString();
      session.setAttribute("checkoutToken", token);
    }
    f.token = token;
    m.addAttribute("form", f);
    checkoutModel(p, m);
    return "checkout";
  }

  @PostMapping("/checkout")
  String checkout(
      Principal p,
      @Valid @ModelAttribute("form") Forms.Checkout f,
      BindingResult r,
      Model m,
      HttpSession s) {
    if (r.hasErrors()) {
      checkoutModel(p, m);
      return "checkout";
    }
    var existing = orderRepo.findByCheckoutToken(f.token);
    if (existing.isEmpty() && !f.token.equals(s.getAttribute("checkoutToken")))
      throw new IllegalArgumentException("Phiên đặt hàng đã hết hạn. Mở lại giỏ hàng");
    ShopOrder o = orders.checkout(uid(p), f);
    s.removeAttribute("checkoutToken");
    return "redirect:/orders/" + o.id;
  }

  @GetMapping("/orders")
  String orders(Principal p, Model m) {
    m.addAttribute("orders", orderRepo.findByAccountIdOrderByCreatedAtDesc(uid(p)));
    return "orders";
  }

  @GetMapping("/orders/{id}")
  String order(Principal p, @PathVariable long id, Model m) {
    m.addAttribute("order", orders.owned(uid(p), id));
    m.addAttribute("items", items.findByOrderIdOrderByProductIdAsc(id));
    return "order";
  }

  @PostMapping("/orders/{id}/cancel")
  String cancel(Principal p, @PathVariable long id) {
    orders.cancel(uid(p), id);
    return "redirect:/orders/" + id;
  }

  @PostMapping("/orders/{id}/pay")
  String pay(Principal p, @PathVariable long id, HttpServletRequest req) {
    return "redirect:" + payments.start(uid(p), id, req.getRemoteAddr());
  }

  @PostMapping("/reviews")
  String review(
      Principal p,
      @RequestParam long productId,
      @RequestParam int rating,
      @RequestParam String content) {
    reviewService.add(uid(p), productId, rating, content);
    return "redirect:/products/" + productId;
  }

  @GetMapping("/payments/return")
  String result() {
    return "payment-return";
  }
}
