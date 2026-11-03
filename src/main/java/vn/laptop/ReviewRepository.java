package vn.laptop;

import org.springframework.data.jpa.repository.*;

public interface ReviewRepository extends JpaRepository<Review, Long> {
  java.util.List<Review> findByProductIdOrderByCreatedAtDesc(Long productId);

  boolean existsByAccountIdAndProductId(Long accountId, Long productId);
}
