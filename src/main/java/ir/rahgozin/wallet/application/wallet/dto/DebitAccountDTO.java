package ir.rahgozin.wallet.application.wallet.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DebitAccountDTO {
    private String account;
    private BigDecimal amount;
}
