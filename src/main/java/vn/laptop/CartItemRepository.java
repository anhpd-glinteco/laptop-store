package vn.laptop;

import org.springframework.data.jpa.repository.*;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
  java.util.List<CartItem> findByAccountIdOrderByProductIdAsc(Long accountId);

  java.util.Optional<CartItem> findByAccountIdAndProductId(Long accountId, Long productId);

  void deleteByAccountId(Long accountId);
}
