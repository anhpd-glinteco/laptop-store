package vn.laptop;

import java.util.*;

public interface OrderService {
  ShopOrder checkout(long uid, Forms.Checkout form);

  ShopOrder owned(long uid, long id);

  void transition(long id, String next);

  void cancel(long uid, long id);

  String paymentResult(
      String ref, String provider, long amount, boolean success, String transaction);

  void expire(long id);
}
