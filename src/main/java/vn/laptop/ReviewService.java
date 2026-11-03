package vn.laptop;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {
  private final ReviewRepository reviews;
  private final OrderItemRepository items;
  private final AccountRepository accounts;

  public ReviewService(ReviewRepository r, OrderItemRepository i, AccountRepository a) {
    reviews = r;
    items = i;
    accounts = a;
  }

  @Transactional
  public void add(long uid, long pid, int rating, String content) {
    Account a = accounts.locked(uid).orElseThrow();
    if (rating < 1 || rating > 5 || content == null || content.isBlank() || content.length() > 1500)
      throw new IllegalArgumentException("Đánh giá cần 1–5 sao, bình luận 1–1500 ký tự");
    if (items.purchased(uid, pid) == 0)
      throw new IllegalArgumentException("Chỉ được đánh giá sau khi đơn đã giao");
    if (reviews.existsByAccountIdAndProductId(uid, pid))
      throw new IllegalArgumentException("Bạn đã đánh giá sản phẩm này");
    Review r = new Review();
    r.accountId = uid;
    r.productId = pid;
    r.rating = rating;
    r.content = content.trim();
    r.author = a.fullName;
    reviews.save(r);
  }
}
