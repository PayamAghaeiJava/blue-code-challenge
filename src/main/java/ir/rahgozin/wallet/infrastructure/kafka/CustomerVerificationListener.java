package ir.rahgozin.wallet.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ir.rahgozin.wallet.application.customer.kyc.CustomerVerificationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomerVerificationListener {
    private final CustomerVerificationService customerVerificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${wallet.kyc.topic:customer_verification_topic}")
    public void handleKycVerificationEvent(String message) throws JsonProcessingException {
        KycVerificationEvent event = objectMapper.readValue(message, KycVerificationEvent.class);
        if (event == null || event.getIdentifierType() == null || event.getIdentifierType().isBlank()
                || event.getIdentifierValue() == null || event.getIdentifierValue().isBlank()
                || event.getVerified() == null) {
            throw new IllegalArgumentException("Invalid KYC verification event");
        }

        switch (event.getIdentifierType()) {
            case "ID":
                customerVerificationService.handleByCustomerId(event.getIdentifierValue(), event.getVerified());
                break;
            case "NATIONAL_CODE":
                customerVerificationService.handleByNationalCode(event.getIdentifierValue(), event.getVerified());
                break;
            default:
                throw new IllegalArgumentException("Unknown KYC identifier type: " + event.getIdentifierType());
        }
    }

    @Data
    public static class KycVerificationEvent {
        private String identifierType;
        private String identifierValue;
        private Boolean verified;
    }

}
