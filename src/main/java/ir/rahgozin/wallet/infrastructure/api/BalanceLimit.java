package ir.rahgozin.wallet.infrastructure.api;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

@Component
public class BalanceLimit {
    private final Map<Long, Integer> customerIdRequestCounts = new HashMap<>();
    private LocalDate lastDate;

    public synchronized boolean isAllowed(Long customerId) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if (!today.equals(lastDate)) {
            customerIdRequestCounts.clear();
            lastDate = today;
        }

        int count = customerIdRequestCounts.getOrDefault(customerId,0);
        if (count >= 5) {
            return false;
        }
        customerIdRequestCounts.put(customerId, count + 1);
        return true;
    }
}
