package ir.rahgozin.wallet.infrastructure.api;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BalanceLimitTest {
    @Test
    void limitIsPerCustomerAndResetsAtMidnightUtc() {
        BalanceLimit limit = new BalanceLimit();
        for (int i = 0; i < 5; i++) {
            assertTrue(limit.isAllowed(1L));
        }
        assertFalse(limit.isAllowed(1L));
        assertTrue(limit.isAllowed(2L));

        ReflectionTestUtils.setField(limit, "lastDate", LocalDate.now(ZoneOffset.UTC).minusDays(1));
        assertTrue(limit.isAllowed(1L));
    }
}
