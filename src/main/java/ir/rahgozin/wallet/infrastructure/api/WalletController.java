package ir.rahgozin.wallet.infrastructure.api;

import ir.rahgozin.wallet.application.wallet.WalletService;
import ir.rahgozin.wallet.application.wallet.command.CreateAccountCommand;
import ir.rahgozin.wallet.application.wallet.command.CreditAccountCommand;
import ir.rahgozin.wallet.application.wallet.command.DebitAccountCommand;
import ir.rahgozin.wallet.application.wallet.query.AccountBalanceQuery;
import ir.rahgozin.wallet.application.wallet.query.TransactionQuery;
import ir.rahgozin.wallet.infrastructure.api.resources.CreateAccountRequest;
import ir.rahgozin.wallet.infrastructure.api.resources.CreditAccountRequest;
import ir.rahgozin.wallet.infrastructure.api.resources.DebitAccountRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@Log4j2
@RestController
@RequestMapping(value = "/wallet")
public class WalletController {
    @Autowired
    private WalletService walletService;
    @Autowired
    private ConversionService conversionService;
    @Autowired
    private BalanceLimit balanceLimit;


    @PostMapping(path = "/create")
    public ResponseEntity<?> createAccount(@RequestBody CreateAccountRequest request) {

        CreateAccountCommand createAccountCommand = conversionService.convert(request, CreateAccountCommand.class);

        return ResponseEntity.ok(walletService.createAccount(createAccountCommand));
    }

    @PostMapping(path = "/credit")
    public ResponseEntity<?> creditAccount(@RequestBody CreditAccountRequest request) {

        CreditAccountCommand creditAccountCommand = conversionService.convert(request, CreditAccountCommand.class);

        return ResponseEntity.ok(walletService.creditAccount(creditAccountCommand));
    }

    @PostMapping(path = "/debit")
    public ResponseEntity<?> debitAccount(@RequestBody DebitAccountRequest request) {
        DebitAccountCommand debitAccountCommand = conversionService.convert(request, DebitAccountCommand.class);
        return ResponseEntity.ok(walletService.debitAccount(debitAccountCommand));
    }

    @PostMapping(path = "/balance")
    public ResponseEntity<?> accountBalance(@RequestParam("customerId") Long customerId) {
        if (!balanceLimit.isAllowed(customerId)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        AccountBalanceQuery query = new AccountBalanceQuery();
        query.setCustomerId(customerId);
        return ResponseEntity.ok(walletService.accountBalance(query));
    }

    @PostMapping(path = "/transactions")
    public ResponseEntity<?> transactionList(@RequestParam("customerId") Long customerId,
                                             @RequestParam(value = "fromAmount", required = false) BigDecimal fromAmount,
                                             @RequestParam(value = "toAmount", required = false) BigDecimal toAmount,
                                             @RequestParam(value = "direction", required = false) String direction) {
        TransactionQuery query = new TransactionQuery();
        query.setCustomerId(customerId);
        query.setFromAmount(fromAmount);
        query.setToAmount(toAmount);
        query.setDirection(direction);
        return ResponseEntity.ok(walletService.listTransactions(query));
    }
}
