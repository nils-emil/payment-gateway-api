package com.paymentgateway.domain.service;

import com.paymentgateway.domain.model.*;
import com.paymentgateway.domain.port.in.PaymentRequest;
import com.paymentgateway.domain.port.out.AcquiringBankClient;
import com.paymentgateway.domain.port.out.BankResult;
import com.paymentgateway.domain.port.out.PaymentRepository;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ProcessPaymentServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-15T00:00:00Z"), ZoneOffset.UTC);
    private final CurrencyAllowList allowList = new CurrencyAllowList(Set.of("GBP", "USD", "EUR"));

    private final List<Payment> saved = new ArrayList<>();
    private final PaymentRepository repository = new PaymentRepository() {
        public Payment save(Payment p) { saved.add(p); return p; }
        public Optional<Payment> findById(UUID id) {
            return saved.stream().filter(x -> x.id().equals(id)).reduce((a, b) -> b);
        }
    };

    private AcquiringBankClient bankReturning(BankResult result) {
        return (card, money) -> result;
    }

    private PaymentRequest validRequest(String cardNumber) {
        return new PaymentRequest(cardNumber, 4, 2027, "GBP", 100, "123");
    }

    @Test
    void authorizedPaymentIsPersistedPendingThenAuthorized() {
        var service = new ProcessPaymentService(repository, bankReturning(new BankResult(true, "auth-1")), allowList, clock);

        Payment result = service.process(validRequest("2222405343248877"));

        assertEquals(PaymentStatus.AUTHORIZED, result.status());
        assertEquals("auth-1", result.authorizationCode());
        assertEquals("8877", result.lastFour());
        // persist-first: first save Pending, second save Authorized, same id
        assertEquals(2, saved.size());
        assertEquals(PaymentStatus.PENDING, saved.get(0).status());
        assertEquals(PaymentStatus.AUTHORIZED, saved.get(1).status());
        assertEquals(saved.get(0).id(), saved.get(1).id());
    }

    @Test
    void declinedPaymentIsPersistedDeclined() {
        var service = new ProcessPaymentService(repository, bankReturning(new BankResult(false, null)), allowList, clock);

        Payment result = service.process(validRequest("2222405343248878"));

        assertEquals(PaymentStatus.DECLINED, result.status());
        assertEquals(2, saved.size());
        assertEquals(PaymentStatus.PENDING, saved.get(0).status());
        assertEquals(PaymentStatus.DECLINED, saved.get(1).status());
        assertEquals(saved.get(0).id(), saved.get(1).id());
    }

    @Test
    void invalidRequestThrowsAndNeverPersistsOrCallsBank() {
        AcquiringBankClient explodingBank = (card, money) -> { throw new AssertionError("bank must not be called"); };
        var service = new ProcessPaymentService(repository, explodingBank, allowList, clock);

        PaymentRequest bad = new PaymentRequest("123", 13, 2020, "ZZZ", 0, "1");
        ValidationException ex = assertThrows(ValidationException.class, () -> service.process(bad));

        assertTrue(saved.isEmpty());
        assertTrue(ex.errors().size() >= 4, "expected aggregated errors, got " + ex.errors());
    }

    @Test
    void unsupportedCurrencyIsRejected() {
        var service = new ProcessPaymentService(repository, bankReturning(new BankResult(true, "x")), allowList, clock);
        PaymentRequest req = new PaymentRequest("2222405343248877", 4, 2027, "JPY", 100, "123");
        ValidationException ex = assertThrows(ValidationException.class, () -> service.process(req));
        assertTrue(ex.errors().toString().contains("JPY"));
        assertTrue(saved.isEmpty());
    }

    @Test
    void bankUnavailableLeavesPaymentPending() {
        AcquiringBankClient downBank = (card, money) -> { throw new BankUnavailableException("down", null); };
        var service = new ProcessPaymentService(repository, downBank, allowList, clock);

        assertThrows(BankUnavailableException.class, () -> service.process(validRequest("2222405343248870")));
        assertEquals(1, saved.size());
        assertEquals(PaymentStatus.PENDING, saved.get(0).status());
    }
}
