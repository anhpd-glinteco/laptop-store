package vn.laptop;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ShopOrderRepository extends JpaRepository<ShopOrder, Long> {
  java.util.List<ShopOrder> findByAccountIdOrderByCreatedAtDesc(Long accountId);

  java.util.List<ShopOrder> findAllByOrderByCreatedAtDesc();

  java.util.Optional<ShopOrder> findByCheckoutToken(String checkoutToken);

  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select o from ShopOrder o where o.id=:id")
  java.util.Optional<ShopOrder> locked(@Param("id") Long id);

  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select o from ShopOrder o where o.paymentRef=:ref")
  java.util.Optional<ShopOrder> lockRef(@Param("ref") String ref);

  @Query(
      "select o.id from ShopOrder o where o.paymentStatus='PENDING' and o.paymentMethod<>'COD' and"
          + " o.status='NEW' and o.expiresAt<:now")
  java.util.List<Long> expired(@Param("now") java.time.LocalDateTime now);
}
