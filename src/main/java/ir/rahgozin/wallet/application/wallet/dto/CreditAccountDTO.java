package ir.rahgozin.wallet.application.wallet.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;


@AllArgsConstructor
public class CreditAccountDTO {
    private String accountNumber;
    private BigDecimal amount;
}
