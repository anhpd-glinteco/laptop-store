package vn.laptop;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
  java.util.List<OrderItem> findByOrderIdOrderByProductIdAsc(Long orderId);

  @Query(
      "select count(i) from OrderItem i, ShopOrder o where i.orderId=o.id and o.accountId=:uid and"
          + " i.productId=:pid and o.status='DELIVERED'")
  long purchased(@Param("uid") Long uid, @Param("pid") Long pid);
}
