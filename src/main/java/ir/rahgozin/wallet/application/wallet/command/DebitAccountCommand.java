package ir.rahgozin.wallet.application.wallet.command;

import lombok.Data;

@Data
public class DebitAccountCommand {
    private String requestId;
    private String accountNumber;
    private Double amount;
}
