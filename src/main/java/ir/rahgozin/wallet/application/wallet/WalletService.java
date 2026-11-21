package ir.rahgozin.wallet.application.wallet;

import ir.rahgozin.wallet.application.common.NotificationService;
import ir.rahgozin.wallet.application.customer.Customer;
import ir.rahgozin.wallet.application.customer.CustomerRepository;
import ir.rahgozin.wallet.application.wallet.Account.AccountType;
import ir.rahgozin.wallet.application.wallet.command.CreateAccountCommand;
import ir.rahgozin.wallet.application.wallet.command.CreditAccountCommand;
import ir.rahgozin.wallet.application.wallet.command.DebitAccountCommand;
import ir.rahgozin.wallet.application.wallet.dto.AccountBalanceDTO;
import ir.rahgozin.wallet.application.wallet.dto.CreditAccountDTO;
import ir.rahgozin.wallet.application.wallet.dto.DebitAccountDTO;
import ir.rahgozin.wallet.application.wallet.dto.TransactionDTO;
import ir.rahgozin.wallet.application.wallet.query.AccountBalanceQuery;
import ir.rahgozin.wallet.application.wallet.query.TransactionQuery;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Log4j2
@Service
@AllArgsConstructor
public class WalletService {
    private AccountRepository accountRepository;
    private CustomerRepository customerRepository;
    private TransactionRepository transactionRepository;
    private NotificationService notificationService;

    // readme.md: accounts have one owner. Create customers separately, then attach one here would be second solution and even a better idea.
    // also if only one method is responsible for a persist , it has full responsibility of it's failure
    @Transactional
    public String createAccount(CreateAccountCommand command) {
        Customer customer = customerRepository.findByNationalCode(command.getNationalCode());
        if (customer == null) {
            Customer newCustomer = new Customer();
            newCustomer.setMobile(command.getMobile());
            newCustomer.setFirstName(command.getFirstName());
            newCustomer.setLastName(command.getLastName());
            newCustomer.setNationalCode(command.getNationalCode());
            customer = customerRepository.save(newCustomer);
        }

        Account account = new Account();
        account.setOwner(customer);
        account.setType(AccountType.valueOf(command.getType()));
        account.setNumber(UUID.randomUUID().toString());
        accountRepository.save(account);
        return account.getNumber();
    }

    @Transactional
    public CreditAccountDTO creditAccount(CreditAccountCommand command) {
        validateAmount(command.getAmount());
        Account account = findVerifiedAccount(command.getAccountNumber());

        Transaction transaction = new Transaction(
                command.getRequestId(),
                account,
                TransactionDirection.CREDIT,
                command.getAmount());

        account.addTransaction(transaction);
        transactionRepository.save(transaction);

        notificationService.send(account.getOwner().getMobile(), "deposit request of " + command.getAmount() + " has been successfully processed.");

        accountRepository.save(account);

        return new CreditAccountDTO(
                account.getNumber(),
                transaction.getAmount());
    }

    @Transactional
    public DebitAccountDTO debitAccount(DebitAccountCommand command) {
        validateAmount(command.getAmount());
        Account account = findVerifiedAccount(command.getAccountNumber());
        if (account.getType() != AccountType.CHECKING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only checking accounts can be debited");
        }
        if (calculateBalance(account).compareTo(command.getAmount()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
        }

        Transaction transaction = new Transaction(
                command.getRequestId(),
                account,
                TransactionDirection.DEBIT,
                command.getAmount()
        );

        account.addTransaction(transaction);
        transactionRepository.save(transaction);

        notificationService.send(account.getOwner().getMobile(), "withdrawal request of " + command.getAmount() + " has been successfully processed.");

        accountRepository.save(account);

        return new DebitAccountDTO(account.getNumber(), command.getAmount());
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.stripTrailingZeros().scale() > 2 || amount.abs().compareTo(new BigDecimal("100000000000000000")) >= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must fit within 17 digits");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be greater than zero");
        }
    }

    private Account findVerifiedAccount(String accountNumber) {
        Account account = accountRepository.findByNumber(accountNumber);
        if (account == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found: " + accountNumber);
        }
        if (account.getOwner() == null || !account.getOwner().isVerified()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account owner must be verified");
        }
        return account;
    }

    @Transactional(readOnly = true)
    public List<AccountBalanceDTO> accountBalance(AccountBalanceQuery query) {
        List<AccountBalanceDTO> balances = new ArrayList<>();
        List<Account> accounts = accountRepository.findByOwnerId(query.getCustomerId());

        for (Account account : accounts) {
            AccountBalanceDTO result = new AccountBalanceDTO();
            result.setNumber(account.getNumber());
            result.setAmount(calculateBalance(account));
            balances.add(result);
        }
        return balances;
    }

    private BigDecimal calculateBalance(Account account) {
        BigDecimal balance = BigDecimal.ZERO;
        for (Transaction transaction : account.getTransactions()) {
            if (transaction.getDirection() == TransactionDirection.CREDIT) {
                balance = balance.add(transaction.getAmount());
            } else if (transaction.getDirection() == TransactionDirection.DEBIT) {
                balance = balance.subtract(transaction.getAmount());
            } else {
                throw new IllegalStateException("Unknown transaction direction: " + transaction.getDirection());
            }
        }
        return balance;
    }

    @Transactional(readOnly = true)
    public List<TransactionDTO> listTransactions(TransactionQuery query) {
        if (query.getCustomerId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer ID is required");
        }
        if (query.getFromAmount() != null && query.getToAmount() != null
                && query.getFromAmount().compareTo(query.getToAmount()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Minimum amount must not exceed maximum amount");
        }

        TransactionDirection direction = null;
        if (query.getDirection() != null && !query.getDirection().isBlank()) {
            if (query.getDirection().equalsIgnoreCase("deposit")) {
                direction = TransactionDirection.CREDIT;
            } else if (query.getDirection().equalsIgnoreCase("withdrawal")) {
                direction = TransactionDirection.DEBIT;
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Direction must be deposit or withdrawal");
            }
        }

        List<TransactionDTO> results = new ArrayList<>();
        for (Account account : accountRepository.findByOwnerId(query.getCustomerId())) {
            for (Transaction transaction : account.getTransactions()) {
                if (direction != null && transaction.getDirection() != direction) {
                    continue;
                }
                if (query.getFromAmount() != null && transaction.getAmount().compareTo(query.getFromAmount()) < 0) {
                    continue;
                }
                if (query.getToAmount() != null && transaction.getAmount().compareTo(query.getToAmount()) > 0) {
                    continue;
                }
                results.add(new TransactionDTO(account.getId(), transaction.getAmount()));
            }
        }
        return results;
    }
}
