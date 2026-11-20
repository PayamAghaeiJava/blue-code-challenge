package ir.rahgozin.wallet.infrastructure.api.resources;

import lombok.Data;

@Data
public class DebitAccountRequest {
    private String requestId;
    private String accountNumber;
    private Double amount;
}
