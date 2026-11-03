package vn.laptop;

import jakarta.validation.Valid;
import java.time.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/admin")
public class AdminController {
  private final CatalogService catalog;
  private final ProductRepository products;
  private final CategoryDao categories;
  private final ShopOrderRepository orders;
  private final OrderItemRepository items;
  private final OrderService service;
  private final ReportDao reports;
  private final ImageStorage images;

  public AdminController(
      CatalogService c,
      ProductRepository p,
      CategoryDao ca,
      ShopOrderRepository o,
      OrderItemRepository i,
      OrderService s,
      ReportDao r,
      ImageStorage im) {
    catalog = c;
    products = p;
    categories = ca;
    orders = o;
    items = i;
    service = s;
    reports = r;
    images = im;
  }

  @GetMapping
  String dashboard(
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      Model m) {
    if (from == null) from = LocalDate.now().withDayOfMonth(1);
    if (to == null) to = LocalDate.now();
    m.addAttribute("from", from);
    m.addAttribute("to", to);
    m.addAttribute("summary", reports.summary(from, to));
    m.addAttribute("best", reports.best(from, to));
    m.addAttribute("daily", reports.daily(from, to));
    m.addAttribute("products", products.count());
    m.addAttribute("today", LocalDate.now());
    return "admin/dashboard";
  }

  @GetMapping("/products")
  String products(Model m) {
    m.addAttribute(
        "products", products.findAll(org.springframework.data.domain.Sort.by("id").descending()));
    return "admin/products";
  }

  @GetMapping("/products/new")
  String create(Model m) {
    m.addAttribute("form", new Forms.ProductForm());
    m.addAttribute("categories", categories.findAll());
    return "admin/product-form";
  }

  @GetMapping("/products/{id}/edit")
  String edit(@PathVariable long id, Model m) {
    Product p = catalog.get(id);
    Forms.ProductForm f = new Forms.ProductForm();
    org.springframework.beans.BeanUtils.copyProperties(p, f);
    m.addAttribute("form", f);
    m.addAttribute("categories", categories.findAll());
    return "admin/product-form";
  }

  @PostMapping("/products")
  String save(
      @Valid @ModelAttribute("form") Forms.ProductForm f,
      BindingResult r,
      @RequestParam(required = false) MultipartFile image,
      Model m) {
    if (r.hasErrors()) {
      m.addAttribute("categories", categories.findAll());
      return "admin/product-form";
    }
    catalog.save(f, images.save(image));
    return "redirect:/admin/products";
  }

  @PostMapping("/products/{id}/hide")
  String hide(@PathVariable long id) {
    catalog.hide(id);
    return "redirect:/admin/products";
  }

  @GetMapping("/categories")
  String categories(Model m) {
    m.addAttribute("categories", categories.findAll());
    return "admin/categories";
  }

  @PostMapping("/categories")
  String createCategory(@RequestParam String name) {
    categories.create(name);
    return "redirect:/admin/categories";
  }

  @PostMapping("/categories/{id}")
  String editCategory(@PathVariable long id, @RequestParam String name) {
    categories.update(id, name);
    return "redirect:/admin/categories";
  }

  @PostMapping("/categories/{id}/delete")
  String deleteCategory(@PathVariable long id) {
    categories.delete(id);
    return "redirect:/admin/categories";
  }

  @GetMapping("/orders")
  String orders(Model m) {
    m.addAttribute("orders", orders.findAllByOrderByCreatedAtDesc());
    return "admin/orders";
  }

  @GetMapping("/orders/{id}")
  String order(@PathVariable long id, Model m) {
    m.addAttribute("order", orders.findById(id).orElseThrow());
    m.addAttribute("items", items.findByOrderIdOrderByProductIdAsc(id));
    return "admin/order";
  }

  @PostMapping("/orders/{id}/status")
  String status(@PathVariable long id, @RequestParam String status) {
    service.transition(id, status);
    return "redirect:/admin/orders/" + id;
  }
}
