package ir.rahgozin.wallet.application.wallet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Account findByNumber(String number);
    List<Account> findByOwnerId(Long ownerId);
}
