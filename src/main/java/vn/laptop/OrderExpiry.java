package vn.laptop;

import java.time.LocalDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderExpiry {
  private final ShopOrderRepository orders;
  private final OrderService service;

  public OrderExpiry(ShopOrderRepository r, OrderService s) {
    orders = r;
    service = s;
  }

  @Scheduled(fixedDelay = 60000)
  public void sweep() {
    for (Long id : orders.expired(LocalDateTime.now())) service.expire(id);
  }
}
