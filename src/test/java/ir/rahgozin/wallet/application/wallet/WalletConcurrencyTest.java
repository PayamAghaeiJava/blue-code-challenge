package ir.rahgozin.wallet.application.wallet;

import ir.rahgozin.wallet.application.common.NotificationService;
import ir.rahgozin.wallet.application.customer.Customer;
import ir.rahgozin.wallet.application.customer.CustomerRepository;
import ir.rahgozin.wallet.application.wallet.command.DebitAccountCommand;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@Import(WalletService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class WalletConcurrencyTest {
    @Autowired WalletService walletService;
    @Autowired AccountRepository accountRepository;
    @Autowired CustomerRepository customerRepository;
    @Autowired TransactionRepository transactionRepository;
    @MockBean NotificationService notificationService;

    @Test
    void givenTwoWithdrawals_whenTogether_thenOnlyOneCanSpendTheBalance() throws Exception {
        Customer customer = new Customer();
        customer.setNationalCode("concurrency-customer");
        customer.setMobile("09120000000");
        customer.setVerified(true);
        customerRepository.save(customer);

        Account account = new Account();
        account.setOwner(customer);
        account.setType(Account.AccountType.CHECKING);
        accountRepository.save(account);
        transactionRepository.save(new Transaction("initial-credit", account,
                TransactionDirection.CREDIT, new BigDecimal("100.00")));

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> withdraw(account.getNumber(), "debit-1", start));
            Future<Boolean> second = executor.submit(() -> withdraw(account.getNumber(), "debit-2", start));
            start.countDown();
            int successes = (first.get(15, TimeUnit.SECONDS) ? 1 : 0)
                    + (second.get(15, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, successes);
            assertEquals(2, transactionRepository.count());
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean withdraw(String accountNumber, String requestId, CountDownLatch start) throws InterruptedException {
        DebitAccountCommand command = new DebitAccountCommand();
        command.setAccountNumber(accountNumber);
        command.setRequestId(requestId);
        command.setAmount(new BigDecimal("80.00"));
        start.await();
        try {
            walletService.debitAccount(command);
            return true;
        } catch (ResponseStatusException exception) {
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            return false;
        }
    }
}
