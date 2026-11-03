package vn.laptop;

import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReportDao {
  private final JdbcTemplate jdbc;

  public ReportDao(JdbcTemplate j) {
    jdbc = j;
  }

  public Map<String, Object> summary(LocalDate from, LocalDate to) {
    if (to.isBefore(from) || to.isAfter(from.plusYears(5)))
      throw new IllegalArgumentException("Khoảng ngày không hợp lệ (tối đa 5 năm)");
    return jdbc.queryForMap(
        "SELECT COUNT(*) AS order_count, COALESCE(SUM(total),0) AS revenue FROM shop_orders WHERE"
            + " status='DELIVERED' AND payment_status='PAID' AND delivered_at>=? AND"
            + " delivered_at<?",
        from.atStartOfDay(),
        to.plusDays(1).atStartOfDay());
  }

  public List<Map<String, Object>> best(LocalDate from, LocalDate to) {
    return jdbc.queryForList(
        "SELECT i.product_id, i.product_name, SUM(i.quantity) AS quantity FROM order_items i JOIN"
            + " shop_orders o ON o.id=i.order_id WHERE o.status='DELIVERED' AND"
            + " o.payment_status='PAID' AND o.delivered_at>=? AND o.delivered_at<? GROUP BY"
            + " i.product_id,i.product_name ORDER BY quantity DESC LIMIT 10",
        from.atStartOfDay(),
        to.plusDays(1).atStartOfDay());
  }

  public List<Map<String, Object>> daily(LocalDate from, LocalDate to) {
    return jdbc.queryForList(
        "SELECT CAST(delivered_at AS DATE) AS report_day, SUM(total) AS revenue FROM shop_orders"
            + " WHERE status='DELIVERED' AND payment_status='PAID' AND delivered_at>=? AND"
            + " delivered_at<? GROUP BY CAST(delivered_at AS DATE) ORDER BY report_day",
        from.atStartOfDay(),
        to.plusDays(1).atStartOfDay());
  }
}
