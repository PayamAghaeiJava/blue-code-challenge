package ir.rahgozin.wallet.infrastructure.api.resources;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class DebitAccountRequest {
    private String requestId;
    private String accountNumber;
    private BigDecimal amount;
}
