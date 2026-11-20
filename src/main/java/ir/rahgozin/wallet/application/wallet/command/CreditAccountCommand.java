package ir.rahgozin.wallet.application.wallet.command;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class CreditAccountCommand {
    private String requestId;
    private String accountNumber;
    private BigDecimal amount;

}
