package ir.rahgozin.wallet.application.wallet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.server.ResponseStatusException;

import ir.rahgozin.wallet.application.common.NotificationService;
import ir.rahgozin.wallet.application.customer.Customer;
import ir.rahgozin.wallet.application.customer.CustomerRepository;
import ir.rahgozin.wallet.application.wallet.command.CreateAccountCommand;
import ir.rahgozin.wallet.application.wallet.command.CreditAccountCommand;
import ir.rahgozin.wallet.application.wallet.command.DebitAccountCommand;
import ir.rahgozin.wallet.application.wallet.dto.AccountBalanceDTO;
import ir.rahgozin.wallet.application.wallet.dto.TransactionDTO;
import ir.rahgozin.wallet.application.wallet.query.AccountBalanceQuery;
import ir.rahgozin.wallet.application.wallet.query.TransactionQuery;


@SpringJUnitConfig({
        WalletService.class
})
public class WalletServiceTest {

    @Autowired
    WalletService walletService;

    @MockBean
    AccountRepository accountRepository;

    @MockBean
    CustomerRepository customerRepository;

    @MockBean
    TransactionRepository transactionRepository;

    @MockBean
    NotificationService notificationService;

    @Test
    void givenNewCustomer_whenCreateAccount_thenSaveCustomerBeforeAccount() {
        CreateAccountCommand command = new CreateAccountCommand();
        command.setNationalCode("1234567890");
        command.setFirstName("Ada");
        command.setLastName("Lovelace");
        command.setMobile("09120000000");
        command.setType("CHECKING");

        Mockito.when(customerRepository.save(Mockito.any(Customer.class))).thenAnswer(invocation -> {
            Customer savedCustomer = invocation.getArgument(0);
            savedCustomer.setId(42L);
            return savedCustomer;
        });
        Mockito.when(accountRepository.save(Mockito.any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String accountNumber = walletService.createAccount(command);

        Mockito.verify(customerRepository).save(Mockito.argThat(customer ->
                "1234567890".equals(customer.getNationalCode()) && customer.getId() != null));
        Mockito.verify(accountRepository).save(Mockito.argThat(account ->
                account.getOwner().getId().equals(42L)
                        && account.getNumber().equals(accountNumber)));
    }

    @Test
    void givenExistingCustomer_whenCreateAccount_thenReuseCustomer() {
        Customer existingCustomer = new Customer();
        existingCustomer.setId(7L);
        Mockito.when(customerRepository.findByNationalCode("1234567890")).thenReturn(existingCustomer);
        Mockito.when(accountRepository.save(Mockito.any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateAccountCommand command = new CreateAccountCommand();
        command.setNationalCode("1234567890");
        command.setType("CHECKING");

        walletService.createAccount(command);

        Mockito.verify(customerRepository, Mockito.never()).save(Mockito.any(Customer.class));
        Mockito.verify(accountRepository).save(Mockito.argThat(account -> account.getOwner() == existingCustomer));
    }

    @Test
    void givenCredit_whenCreditAccount_thenSaveTransactionWithAccountReference() {
        Account account = new Account();
        Customer owner = new Customer();
        owner.setMobile("09120000000");
        owner.setVerified(true);
        account.setOwner(owner);
        account.setType(Account.AccountType.SAVING);
        Mockito.when(accountRepository.findByNumber(account.getNumber())).thenReturn(account);

        CreditAccountCommand command = new CreditAccountCommand();
        command.setRequestId("credit-1");
        command.setAccountNumber(account.getNumber());
        command.setAmount(new BigDecimal("100.00"));

        walletService.creditAccount(command);

        Mockito.verify(transactionRepository).save(Mockito.argThat(transaction ->
                transaction.getAccount() == account
                        && "credit-1".equals(transaction.getRequestId())
                        && transaction.getAmount().equals(new BigDecimal("100.00"))));
        assertEquals(1, account.getTransactions().size());
    }

    @Test
    void givenDebit_whenDebitAccount_thenSaveTransactionWithAccountReference() {
        Account account = new Account();
        account.setNumber("account-17");
        Customer owner = new Customer();
        owner.setMobile("09120000000");
        owner.setVerified(true);
        account.setOwner(owner);
        account.setType(Account.AccountType.CHECKING);
        Mockito.when(accountRepository.findByNumber("account-17")).thenReturn(account);

        DebitAccountCommand command = new DebitAccountCommand();
        command.setRequestId("debit-1");
        command.setAccountNumber("account-17");
        command.setAmount(new BigDecimal("50.00"));

        walletService.debitAccount(command);

        Mockito.verify(transactionRepository).save(Mockito.argThat(transaction ->
                transaction.getAccount() == account
                        && "debit-1".equals(transaction.getRequestId())
                        && transaction.getAmount().equals(new BigDecimal("50.00"))));
        assertEquals(1, account.getTransactions().size());
    }

    @Test
    void givenUnknownPublicAccountNumber_whenDebit_thenThrowAccountNotFound() {
        DebitAccountCommand command = new DebitAccountCommand();
        command.setAccountNumber("unknown-account");
        command.setRequestId("debit-unknown");
        command.setAmount(new BigDecimal("10.00"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> walletService.debitAccount(command));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        Mockito.verify(accountRepository).findByNumber("unknown-account");
        Mockito.verifyNoInteractions(transactionRepository);
    }

    @Test
    void givenUnverifiedCustomer_whenCreditOrDebit_thenReject() {
        Account account = new Account();
        account.setOwner(new Customer());
        Mockito.when(accountRepository.findByNumber(account.getNumber())).thenReturn(account);
        CreditAccountCommand credit = new CreditAccountCommand();
        credit.setAccountNumber(account.getNumber());
        credit.setAmount(BigDecimal.ONE);
        DebitAccountCommand debit = new DebitAccountCommand();
        debit.setAccountNumber(account.getNumber());
        debit.setAmount(BigDecimal.ONE);

        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
                () -> walletService.creditAccount(credit)).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
                () -> walletService.debitAccount(debit)).getStatusCode());
        Mockito.verifyNoInteractions(transactionRepository, notificationService);
    }

    @Test
    void givenSavingsAccount_whenDebit_thenReject() {
        Account account = new Account();
        Customer owner = new Customer();
        owner.setVerified(true);
        account.setOwner(owner);
        account.setType(Account.AccountType.SAVING);
        Mockito.when(accountRepository.findByNumber(account.getNumber())).thenReturn(account);
        DebitAccountCommand command = new DebitAccountCommand();
        command.setAccountNumber(account.getNumber());
        command.setAmount(BigDecimal.ONE);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> walletService.debitAccount(command));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        Mockito.verifyNoInteractions(transactionRepository, notificationService);
    }


    @Test
    void givenCreditsAndDebits_whenInquiryBalance_thenSubtractDebits() {
        Long customerId = Long.valueOf(RandomStringUtils.randomNumeric(5));

        var accountOwner = new Customer();
        accountOwner.setId(customerId);

        var account = new Account();
        account.setId(1L);
        account.setOwner(accountOwner);


        var debitTransactions =  IntStream.rangeClosed(1, 10)
                .mapToObj(operand ->
                        new Transaction(RandomStringUtils.randomNumeric(5), account, TransactionDirection.CREDIT, new BigDecimal("10000.00") ))
                .collect(Collectors.toList());

        var withdrawalTransactions = IntStream.rangeClosed(1, 5).mapToObj(value ->
                new Transaction(RandomStringUtils.randomNumeric(5), account, TransactionDirection.DEBIT, new BigDecimal("10000.00"))
        ).collect(Collectors.toList());

        List<Transaction> totalTransactions = new ArrayList<>();
        totalTransactions.addAll(debitTransactions);
        totalTransactions.addAll(withdrawalTransactions);
        account.setTransactions(totalTransactions);

        Mockito.doReturn(List.of(account)).when(accountRepository).findByOwnerId(customerId);


        var accountBalanceQuery = new AccountBalanceQuery();
        accountBalanceQuery.setCustomerId(customerId);
        Optional<AccountBalanceDTO> accountBalanceDto = walletService.accountBalance(accountBalanceQuery).stream()
                .filter(accountBalanceDTO -> accountBalanceDTO.getNumber().equals(account.getNumber()))
                .findFirst();

        assertTrue(accountBalanceDto.isPresent());
        assertEquals(new BigDecimal("50000.00"), accountBalanceDto.get().getAmount());
        Mockito.verify(accountRepository).findByOwnerId(customerId);
        Mockito.verify(accountRepository, Mockito.never()).findAll();
    }

    @Test
    void givenDecimalTransactions_whenInquiryBalance_thenCalculateExactly() {
        Account account = new Account();
        account.setNumber("public-number");
        account.setTransactions(List.of(
                new Transaction("one", account, TransactionDirection.CREDIT, new BigDecimal("0.10")),
                new Transaction("two", account, TransactionDirection.CREDIT, new BigDecimal("0.20")),
                new Transaction("three", account, TransactionDirection.DEBIT, new BigDecimal("0.05"))));
        Mockito.when(accountRepository.findByOwnerId(123L)).thenReturn(List.of(account));
        AccountBalanceQuery query = new AccountBalanceQuery();
        query.setCustomerId(123L);

        List<AccountBalanceDTO> balances = walletService.accountBalance(query);

        assertEquals(1, balances.size());
        assertEquals("public-number", balances.get(0).getNumber());
        assertEquals(new BigDecimal("0.25"), balances.get(0).getAmount());
    }

    @Test
    void givenTooManyDecimalPlaces_whenCredit_thenRejectAmount() {
        CreditAccountCommand command = new CreditAccountCommand();
        command.setAmount(new BigDecimal("1.001"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> walletService.creditAccount(command));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        Mockito.verifyNoInteractions(transactionRepository);
    }

    @Test
    void givenTransactionQuery_whenInquiryTransactionList_thenReturnCorrectQueriedList() {
        Long customerId = Long.valueOf(RandomStringUtils.randomNumeric(5));

        var accountOwner = new Customer();
        accountOwner.setId(customerId);

        var account = new Account();
        account.setId(1L);
        account.setOwner(accountOwner);

        var anotherCustomer = new Customer();
        anotherCustomer.setId(Long.valueOf(RandomStringUtils.randomNumeric(5)));

        var anotherAccount = new Account();
        anotherAccount.setId(1L);
        anotherAccount.setOwner(anotherCustomer);

        var mainAccountTransactions =  IntStream.rangeClosed(1, 10)
                .mapToObj(operand -> {
                    if (operand > 5)
                         return new Transaction(RandomStringUtils.randomNumeric(5), account, TransactionDirection.CREDIT, new BigDecimal("10000.00") );
                    else {
                        return new Transaction(RandomStringUtils.randomNumeric(5), account, TransactionDirection.DEBIT, new BigDecimal("5000.00") );
                    }
                })
                .collect(Collectors.toList());
        account.setTransactions(mainAccountTransactions);

        var withdrawalTransactions = IntStream.rangeClosed(1, 5).mapToObj(value ->
                new Transaction(RandomStringUtils.randomNumeric(5), anotherAccount, TransactionDirection.DEBIT, new BigDecimal("10000.00"))
        ).collect(Collectors.toList());
        anotherAccount.setTransactions(withdrawalTransactions);


        Mockito.doReturn(List.of(account, anotherAccount)).when(accountRepository).findAll();

        var transactionQuery = new TransactionQuery();
        transactionQuery.setDirection("deposit");
        transactionQuery.setCustomerId(customerId);
        transactionQuery.setFromAmount(new BigDecimal("6000.00"));


        var transactions = walletService.listTransactions(transactionQuery);
        var sum = transactions.stream().map(TransactionDTO::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        
        assertEquals(new BigDecimal("75000.00"), sum);
    }
}
