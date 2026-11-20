package ir.rahgozin.wallet.application.wallet.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class AccountBalanceDTO {
    private String number;
    private BigDecimal amount;
}
