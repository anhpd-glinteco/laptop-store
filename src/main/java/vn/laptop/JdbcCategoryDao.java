package vn.laptop;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JdbcCategoryDao implements CategoryDao {
  private final JdbcTemplate jdbc;

  public JdbcCategoryDao(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  private final org.springframework.jdbc.core.RowMapper<Category> mapper =
      (rs, n) -> {
        Category c = new Category();
        c.id = rs.getLong("id");
        c.name = rs.getString("name");
        return c;
      };

  public List<Category> findAll() {
    return jdbc.query("SELECT id,name FROM categories ORDER BY name", mapper);
  }

  public Optional<Category> findById(long id) {
    return jdbc.query("SELECT id,name FROM categories WHERE id=?", mapper, id).stream().findFirst();
  }

  private String valid(String name) {
    if (name == null || name.isBlank() || name.trim().length() > 100)
      throw new IllegalArgumentException("Tên danh mục phải có 1–100 ký tự");
    return name.trim();
  }

  public void create(String name) {
    jdbc.update("INSERT INTO categories(name) VALUES(?)", valid(name));
  }

  public void update(long id, String name) {
    if (jdbc.update("UPDATE categories SET name=? WHERE id=?", valid(name), id) != 1)
      throw new IllegalArgumentException("Không tìm thấy danh mục");
  }

  @Transactional
  public void delete(long id) {
    // MySQL FK is the final guard against a concurrent insert.
    if (jdbc.queryForObject("SELECT COUNT(*) FROM products WHERE category_id=?", Long.class, id)
        > 0) throw new IllegalArgumentException("Danh mục đang có sản phẩm, không thể xóa");
    if (jdbc.update("DELETE FROM categories WHERE id=?", id) != 1)
      throw new IllegalArgumentException("Không tìm thấy danh mục");
  }
}
