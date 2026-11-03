package vn.laptop;

import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {
  private final ProductRepository products;
  private final CategoryDao categories;

  public CatalogService(ProductRepository p, CategoryDao c) {
    products = p;
    categories = c;
  }

  public Page<Product> search(
      String q, String brand, String purpose, Long min, Long max, String sort, int page) {
    if (min != null && min < 0 || max != null && max < 0 || min != null && max != null && min > max)
      throw new IllegalArgumentException("Khoảng giá không hợp lệ");
    Sort order =
        switch (sort == null ? "" : sort) {
          case "priceAsc" -> Sort.by("price").ascending();
          case "priceDesc" -> Sort.by("price").descending();
          case "name" -> Sort.by("name");
          default -> Sort.by("id").descending();
        };
    return products.findAll(
        (r, c, b) -> {
          var filters = new ArrayList<jakarta.persistence.criteria.Predicate>();
          filters.add(b.isTrue(r.get("active")));
          if (q != null && !q.isBlank())
            filters.add(
                b.like(
                    b.lower(r.get("name")),
                    "%" + q.trim().toLowerCase(java.util.Locale.ROOT) + "%"));
          if (brand != null && !brand.isBlank()) filters.add(b.equal(r.get("brand"), brand));
          if (purpose != null && !purpose.isBlank())
            filters.add(b.equal(r.get("purpose"), purpose));
          if (min != null) filters.add(b.ge(r.get("price"), min));
          if (max != null) filters.add(b.le(r.get("price"), max));
          return b.and(filters.toArray(new jakarta.persistence.criteria.Predicate[0]));
        },
        PageRequest.of(Math.max(0, page), 12, order));
  }

  public java.util.List<String> brands() {
    return products.brands();
  }

  public Product get(long id) {
    return products
        .findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));
  }

  @Transactional
  public void save(Forms.ProductForm f, String image) {
    categories
        .findById(f.categoryId)
        .orElseThrow(() -> new IllegalArgumentException("Danh mục không tồn tại"));
    Product p = f.id == null ? new Product() : products.locked(f.id).orElseThrow();
    if (f.id != null && !java.util.Objects.equals(p.version, f.version))
      throw new IllegalArgumentException("Sản phẩm vừa thay đổi. Tải lại trang sửa trước khi lưu");
    p.name = f.name;
    p.brand = f.brand;
    p.categoryId = f.categoryId;
    p.purpose = f.purpose;
    p.cpu = f.cpu;
    p.ram = f.ram;
    p.storage = f.storage;
    p.gpu = f.gpu;
    p.screen = f.screen;
    p.description = f.description;
    p.price = f.price;
    p.stock = f.stock;
    p.active = f.active;
    if (image != null) p.imageUrl = image;
    products.save(p);
  }

  @Transactional
  public void hide(long id) {
    Product p = products.locked(id).orElseThrow();
    p.active = false;
  }
}
