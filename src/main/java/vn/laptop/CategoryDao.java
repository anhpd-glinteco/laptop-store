package vn.laptop;

import java.util.*;

public interface CategoryDao {
  List<Category> findAll();

  Optional<Category> findById(long id);

  void create(String name);

  void update(long id, String name);

  void delete(long id);
}
