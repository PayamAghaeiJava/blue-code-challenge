package ir.rahgozin.wallet.application.wallet.query;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class TransactionQuery {
    private Long customerId;
    private BigDecimal fromAmount;
    private BigDecimal toAmount;
    private String direction;
}
