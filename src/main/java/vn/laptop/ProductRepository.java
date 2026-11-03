package vn.laptop;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ProductRepository
    extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
  @Query("select distinct p.brand from Product p where p.active=true order by p.brand")
  java.util.List<String> brands();

  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Product p where p.id=:id")
  java.util.Optional<Product> locked(@Param("id") Long id);
}
