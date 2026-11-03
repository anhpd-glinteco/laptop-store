package vn.laptop;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, Long> {
  java.util.Optional<Account> findByEmail(String email);

  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from Account a where a.id=:id")
  java.util.Optional<Account> locked(@Param("id") Long id);
}
