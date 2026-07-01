package com.paymentgateway.domain.usecase.processpayment;

import com.paymentgateway.domain.support.exception.*;
import com.paymentgateway.domain.model.*;
import com.paymentgateway.domain.port.in.PaymentCommand;
import com.paymentgateway.domain.port.out.AcquiringBankClient;
import com.paymentgateway.domain.port.out.BankResult;
import com.paymentgateway.domain.port.out.IdGenerator;
import com.paymentgateway.domain.port.out.PaymentMetrics;
import com.paymentgateway.domain.port.out.PaymentRepository;
import com.paymentgateway.domain.usecase.processpayment.validation.*;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ProcessPaymentUseCaseTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-15T00:00:00Z"), ZoneOffset.UTC);
    private final CurrencyAllowList allowList = new CurrencyAllowList(Set.of("GBP", "USD", "EUR"));
    private final IdGenerator idGenerator = UUID::randomUUID;

    private final List<Payment> recorded = new ArrayList<>();
    private final PaymentMetrics metrics = recorded::add;

    private final List<Payment> saved = new ArrayList<>();
    private final PaymentRepository repository = new PaymentRepository() {
        public Payment save(Payment p) { saved.add(p); return p; }
        public Optional<Payment> findById(UUID id) {
            return saved.stream().filter(x -> x.id().equals(id)).reduce((a, b) -> b);
        }
        public Optional<Payment> findByIdempotencyKey(String key) {
            return saved.stream().filter(x -> key.equals(x.idempotencyKey())).reduce((a, b) -> b);
        }
    };

    private ProcessPaymentUseCase useCaseWith(AcquiringBankClient bank) {
        List<ValidationRule<PaymentCommand>> rules = List.of(
                new CardNumberValidationRule(),
                new CvvValidationRule(),
                new AmountValidationRule(),
                new ExpiryValidationRule(clock),
                new CurrencyValidationRule(allowList));
        return new ProcessPaymentUseCase(new ProcessPaymentUseCaseSteps(repository, bank, idGenerator, rules), metrics);
    }

    private AcquiringBankClient bankReturning(BankResult result) {
        return command -> result;
    }

    private PaymentCommand validRequest(String cardNumber) {
        return new PaymentCommand(cardNumber, 4, 2027, "GBP", 100L, "123", null);
    }

    @Test
    void authorizedPaymentIsPersistedPendingThenAuthorized() {
        var service = useCaseWith(bankReturning(new BankResult(true, "auth-1")));

        Payment result = service.process(validRequest("2222405343248877"));

        assertEquals(PaymentStatus.AUTHORIZED, result.status());
        assertEquals("auth-1", result.authorizationCode());
        assertEquals("8877", result.lastFour());
        assertEquals("GBP", result.money().currency());
        assertEquals(100, result.money().amount());
        assertEquals(2, saved.size());
        assertEquals(PaymentStatus.PENDING, saved.get(0).status());
        assertEquals(PaymentStatus.AUTHORIZED, saved.get(1).status());
        assertEquals(saved.get(0).id(), saved.get(1).id());
    }

    @Test
    void declinedPaymentIsPersistedDeclined() {
        var service = useCaseWith(bankReturning(new BankResult(false, null)));

        Payment result = service.process(validRequest("2222405343248877"));

        assertEquals(PaymentStatus.DECLINED, result.status());
        assertEquals(2, saved.size());
        assertEquals(PaymentStatus.PENDING, saved.get(0).status());
        assertEquals(PaymentStatus.DECLINED, saved.get(1).status());
        assertEquals(saved.get(0).id(), saved.get(1).id());
    }

    @Test
    void invalidRequestThrowsAndNeverPersistsOrCallsBank() {
        AcquiringBankClient explodingBank = command -> { throw new AssertionError("bank must not be called"); };
        var service = useCaseWith(explodingBank);

        PaymentCommand bad = new PaymentCommand("123", 13, 2020, "ZZZ", 0L, "1", null);
        ValidationException ex = assertThrows(ValidationException.class, () -> service.process(bad));

        assertTrue(saved.isEmpty());
        assertTrue(ex.errors().size() >= 4, "expected aggregated errors, got " + ex.errors());
    }

    @Test
    void unsupportedCurrencyIsRejected() {
        var service = useCaseWith(bankReturning(new BankResult(true, "x")));
        PaymentCommand req = new PaymentCommand("2222405343248877", 4, 2027, "JPY", 100L, "123", null);
        ValidationException ex = assertThrows(ValidationException.class, () -> service.process(req));
        assertTrue(ex.errors().toString().contains("JPY"));
        assertTrue(saved.isEmpty());
    }

    @Test
    void bankFailureLeavesPaymentPending() {
        AcquiringBankClient downBank = command -> { throw new BankUnavailableException("down", null); };
        var service = useCaseWith(downBank);

        assertThrows(BankUnavailableException.class, () -> service.process(validRequest("2222405343248877")));
        assertEquals(1, saved.size());
        assertEquals(PaymentStatus.PENDING, saved.get(0).status());
    }

    @Test
    void idempotentReplayReturnsExistingWithoutCallingBankOrSavingAgain() {
        var service = useCaseWith(bankReturning(new BankResult(true, "auth-1")));
        PaymentCommand first = new PaymentCommand("2222405343248877", 4, 2027, "GBP", 100L, "123", "key-1");
        Payment created = service.process(first);
        int savedAfterFirst = saved.size();

        AcquiringBankClient explodingBank = command -> { throw new AssertionError("bank must not be called on replay"); };
        var replayService = useCaseWith(explodingBank);
        PaymentCommand replay = new PaymentCommand("2222405343248877", 4, 2027, "GBP", 100L, "123", "key-1");
        Payment replayed = replayService.process(replay);

        assertEquals(created.id(), replayed.id());
        assertEquals(savedAfterFirst, saved.size());
    }

    @Test
    void replayWhileInDoubtPendingIsRejectedAndNeverCallsBank() {
        AcquiringBankClient downBank = command -> { throw new BankUnavailableException("down", null); };
        var service = useCaseWith(downBank);
        PaymentCommand first = new PaymentCommand("2222405343248877", 4, 2027, "GBP", 100L, "123", "key-9");
        assertThrows(BankUnavailableException.class, () -> service.process(first));
        assertEquals(1, saved.size());
        assertEquals(PaymentStatus.PENDING, saved.get(0).status());

        AcquiringBankClient explodingBank = command -> { throw new AssertionError("bank must not be called on in-doubt replay"); };
        var replayService = useCaseWith(explodingBank);
        PaymentCommand replay = new PaymentCommand("2222405343248877", 4, 2027, "GBP", 100L, "123", "key-9");
        assertThrows(PaymentInProgressException.class, () -> replayService.process(replay));
        assertEquals(1, saved.size());
    }

    @Test
    void recordsOutcomeMetricOncePerNewPaymentAndNotOnReplay() {
        var service = useCaseWith(bankReturning(new BankResult(true, "auth-1")));
        PaymentCommand first = new PaymentCommand("2222405343248877", 4, 2027, "GBP", 100L, "123", "key-m");
        service.process(first);
        assertEquals(1, recorded.size());
        assertEquals(PaymentStatus.AUTHORIZED, recorded.get(0).status());

        AcquiringBankClient explodingBank = command -> { throw new AssertionError("bank must not be called on replay"); };
        useCaseWith(explodingBank).process(new PaymentCommand("2222405343248877", 4, 2027, "GBP", 100L, "123", "key-m"));
        assertEquals(1, recorded.size());
    }
}
