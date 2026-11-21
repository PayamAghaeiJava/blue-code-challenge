package ir.rahgozin.wallet.application.customer.kyc;

import ir.rahgozin.wallet.application.customer.Customer;
import ir.rahgozin.wallet.application.customer.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerVerificationService {
    private final CustomerRepository customerRepository;

    public void handleByCustomerId(String customerId, boolean verificationResult) {
        Customer customer = customerRepository.findById(Long.valueOf(customerId))
                .orElseThrow(() -> new RuntimeException("customer not found with id " + customerId));
        customer.setVerified(verificationResult);
        customerRepository.save(customer);
    }

    public void handleByNationalCode(String nationalCode, boolean verificationResult) {
        Customer customer = customerRepository.findByNationalCode(nationalCode);
        if (customer == null) {
            throw new IllegalArgumentException("customer not found with national code " + nationalCode);
        }
        customer.setVerified(verificationResult);
        customerRepository.save(customer);
    }
}
