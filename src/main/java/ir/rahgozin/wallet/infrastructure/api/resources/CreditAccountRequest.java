package ir.rahgozin.wallet.infrastructure.api.resources;

import java.math.BigDecimal;

import lombok.Data;


@Data
public class CreditAccountRequest {
    private String accountNumber;
    private BigDecimal amount;
}
