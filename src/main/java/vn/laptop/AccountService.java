package vn.laptop;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
  private final AccountRepository repo;
  private final PasswordEncoder encoder;

  public AccountService(AccountRepository repo, PasswordEncoder encoder) {
    this.repo = repo;
    this.encoder = encoder;
  }

  public Account current(String email) {
    return repo.findByEmail(email)
        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));
  }

  @Transactional
  public Account register(Forms.Register f) {
    String email = f.email.trim().toLowerCase(java.util.Locale.ROOT);
    if (repo.findByEmail(email).isPresent())
      throw new IllegalArgumentException("Email đã được đăng ký");
    Account a = new Account();
    a.email = email;
    a.fullName = f.fullName.trim();
    a.passwordHash = encoder.encode(f.password);
    return repo.save(a);
  }

  @Transactional
  public void profile(Long id, Forms.Profile f) {
    Account a = repo.locked(id).orElseThrow();
    a.fullName = f.fullName.trim();
    a.phone = f.phone;
    a.address = f.address;
  }
}
