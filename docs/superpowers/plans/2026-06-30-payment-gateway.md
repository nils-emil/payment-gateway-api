# Payment Gateway Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Spring Boot payment gateway that validates card payments, forwards valid ones to a simulated acquiring bank, and lets merchants retrieve previously made payments.

**Architecture:** Hexagonal (ports & adapters). A framework-free `domain` package holds the model, use-case ports, and services. Adapters (`in/web`, `out/persistence`, `out/bank`) depend inward on the domain. A persist-first lifecycle saves a `Pending` payment before the bank call and updates it afterward.

**Tech Stack:** Java 21, Spring Boot 3.3.x, Gradle (Groovy DSL), JUnit 5 + Spring Boot Test, OkHttp MockWebServer (bank-adapter integration tests).

## Global Constraints

- Java toolchain: 21. Spring Boot: 3.3.5. Build tool: Gradle wrapper (`./gradlew`).
- `domain` package has **zero** Spring/Jackson/HTTP imports — it must compile standalone.
- Adapters depend on the domain, never the reverse.
- CVV and the full card number are **never** persisted or returned. Only the last 4 digits leave the domain.
- Currency allow-list: `GBP`, `USD`, `EUR` (≤3, from config).
- Application port: `8090`. Bank simulator base URL from config (`payment.bank.base-url`), never hardcoded.
- Two Gradle source sets: `src/test/java` (unit, no Spring) and `src/integrationTest/java` (slice/integration).
- JSON uses snake_case (global Jackson `SNAKE_CASE` strategy) and omits null fields (`non_null` inclusion).
- Payment statuses: `Pending`, `Authorized`, `Declined`. `Rejected` is **not** a stored status — it is the 400 validation outcome only.
- **Git policy (from `CLAUDE.md`): never commit unless the user has authorized it.** The commit steps below are the intended TDD cadence/messages; the executor must either have the user's blanket go-ahead for this plan or stage and pause for approval at each commit step. Never `git push`.

---

### Task 1: Project scaffolding (Gradle, Spring Boot, source sets)

**Files:**
- Create: `build.gradle`
- Create: `settings.gradle`
- Create: `gradle/wrapper/gradle-wrapper.properties` (+ wrapper via `gradle wrapper`)
- Create: `src/main/java/com/paymentgateway/PaymentGatewayApplication.java`
- Create: `src/main/resources/application.yml`
- Test: `src/test/java/com/paymentgateway/PaymentGatewayApplicationTests.java`

**Interfaces:**
- Produces: a buildable Spring Boot app; `./gradlew test` and `./gradlew integrationTest` both runnable.

- [ ] **Step 1: Create `settings.gradle`**

```groovy
rootProject.name = 'payment-gateway-api'
```

- [ ] **Step 2: Create `build.gradle` with the integrationTest source set**

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.3.5'
    id 'io.spring.dependency-management' version '1.1.6'
}

group = 'com.paymentgateway'
version = '0.0.1-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

sourceSets {
    integrationTest {
        java.srcDir 'src/integrationTest/java'
        resources.srcDir 'src/integrationTest/resources'
        compileClasspath += sourceSets.main.output
        runtimeClasspath += sourceSets.main.output
    }
}

configurations {
    integrationTestImplementation.extendsFrom testImplementation
    integrationTestRuntimeOnly.extendsFrom testRuntimeOnly
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    integrationTestImplementation 'com.squareup.okhttp3:mockwebserver:4.12.0'
}

tasks.named('test') {
    useJUnitPlatform()
}

tasks.register('integrationTest', Test) {
    description = 'Runs integration tests.'
    group = 'verification'
    testClassesDirs = sourceSets.integrationTest.output.classesDirs
    classpath = sourceSets.integrationTest.runtimeClasspath
    useJUnitPlatform()
    shouldRunAfter('test')
}

tasks.named('check') {
    dependsOn('integrationTest')
}
```

- [ ] **Step 3: Generate the Gradle wrapper**

Run: `gradle wrapper --gradle-version 8.10`
Expected: `gradlew`, `gradlew.bat`, and `gradle/wrapper/*` created. (If `gradle` is unavailable, copy a wrapper from another project and set `gradle-version=8.10` in `gradle-wrapper.properties`.)

- [ ] **Step 4: Create the application entrypoint**

```java
package com.paymentgateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PaymentGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentGatewayApplication.class, args);
    }
}
```

- [ ] **Step 5: Create `application.yml`**

```yaml
server:
  port: 8090

spring:
  jackson:
    property-naming-strategy: SNAKE_CASE
    default-property-inclusion: non_null

payment:
  bank:
    base-url: http://localhost:8080
  currencies:
    allowed:
      - GBP
      - USD
      - EUR
```

- [ ] **Step 6: Write the context-load test**

```java
package com.paymentgateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PaymentGatewayApplicationTests {
    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 7: Run the build**

Run: `./gradlew clean build`
Expected: PASS — compiles, `contextLoads` passes, both `test` and `integrationTest` tasks run.

- [ ] **Step 8: Commit** (subject to git policy above)

```bash
git add build.gradle settings.gradle gradlew gradlew.bat gradle/ src/ .gitignore
git commit -m "chore: scaffold Spring Boot gradle project with split source sets"
```

---

### Task 2: `Currency` value object

**Files:**
- Create: `src/main/java/com/paymentgateway/domain/model/ValidationException.java`
- Create: `src/main/java/com/paymentgateway/domain/model/Currency.java`
- Test: `src/test/java/com/paymentgateway/domain/model/CurrencyTest.java`

**Interfaces:**
- Produces: `ValidationException(String)` and `ValidationException(List<String>)`, `List<String> errors()`; `Currency.of(String) -> Currency`, `String code()`.

- [ ] **Step 1: Write failing tests**

```java
package com.paymentgateway.domain.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CurrencyTest {
    @Test
    void acceptsThreeLetterUppercaseCode() {
        assertEquals("GBP", Currency.of("GBP").code());
    }

    @Test
    void rejectsNull() {
        ValidationException ex = assertThrows(ValidationException.class, () -> Currency.of(null));
        assertTrue(ex.errors().toString().toLowerCase().contains("currency"));
    }

    @Test
    void rejectsWrongLength() {
        assertThrows(ValidationException.class, () -> Currency.of("GB"));
        assertThrows(ValidationException.class, () -> Currency.of("GBPP"));
    }

    @Test
    void rejectsNonLetters() {
        assertThrows(ValidationException.class, () -> Currency.of("G1P"));
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew test --tests CurrencyTest`
Expected: FAIL — `Currency`/`ValidationException` do not exist.

- [ ] **Step 3: Implement `ValidationException`**

```java
package com.paymentgateway.domain.model;

import java.util.List;

public class ValidationException extends RuntimeException {
    private final List<String> errors;

    public ValidationException(String error) {
        this(List.of(error));
    }

    public ValidationException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> errors() {
        return errors;
    }
}
```

- [ ] **Step 4: Implement `Currency`**

```java
package com.paymentgateway.domain.model;

public record Currency(String code) {
    public static Currency of(String code) {
        if (code == null || !code.matches("[A-Z]{3}")) {
            throw new ValidationException("currency must be a 3-letter ISO 4217 code");
        }
        return new Currency(code);
    }
}
```

- [ ] **Step 5: Run to verify pass**

Run: `./gradlew test --tests CurrencyTest`
Expected: PASS.

- [ ] **Step 6: Commit** (subject to git policy)

```bash
git add src/main/java/com/paymentgateway/domain/model/ src/test/java/com/paymentgateway/domain/model/CurrencyTest.java
git commit -m "feat: add Currency value object and ValidationException"
```

---

### Task 3: `Money` value object

**Files:**
- Create: `src/main/java/com/paymentgateway/domain/model/Money.java`
- Test: `src/test/java/com/paymentgateway/domain/model/MoneyTest.java`

**Interfaces:**
- Consumes: `Currency`, `ValidationException`.
- Produces: `Money.of(Currency, long) -> Money`, `Currency currency()`, `long amount()`.

- [ ] **Step 1: Write failing tests**

```java
package com.paymentgateway.domain.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {
    private final Currency gbp = Currency.of("GBP");

    @Test
    void acceptsPositiveAmount() {
        Money money = Money.of(gbp, 100);
        assertEquals(100, money.amount());
        assertEquals(gbp, money.currency());
    }

    @Test
    void rejectsZeroOrNegative() {
        assertThrows(ValidationException.class, () -> Money.of(gbp, 0));
        assertThrows(ValidationException.class, () -> Money.of(gbp, -5));
    }

    @Test
    void rejectsNullCurrency() {
        assertThrows(ValidationException.class, () -> Money.of(null, 100));
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew test --tests MoneyTest`
Expected: FAIL — `Money` does not exist.

- [ ] **Step 3: Implement `Money`**

```java
package com.paymentgateway.domain.model;

public record Money(Currency currency, long amount) {
    public static Money of(Currency currency, long amount) {
        if (currency == null) {
            throw new ValidationException("currency is required");
        }
        if (amount <= 0) {
            throw new ValidationException("amount must be a positive integer in the minor currency unit");
        }
        return new Money(currency, amount);
    }
}
```

- [ ] **Step 4: Run to verify pass**

Run: `./gradlew test --tests MoneyTest`
Expected: PASS.

- [ ] **Step 5: Commit** (subject to git policy)

```bash
git add src/main/java/com/paymentgateway/domain/model/Money.java src/test/java/com/paymentgateway/domain/model/MoneyTest.java
git commit -m "feat: add Money value object"
```

---

### Task 4: `ExpiryDate` value object

**Files:**
- Create: `src/main/java/com/paymentgateway/domain/model/ExpiryDate.java`
- Test: `src/test/java/com/paymentgateway/domain/model/ExpiryDateTest.java`

**Interfaces:**
- Consumes: `ValidationException`.
- Produces: `ExpiryDate.of(int month, int year, Clock) -> ExpiryDate`, `int month()`, `int year()`.

- [ ] **Step 1: Write failing tests**

```java
package com.paymentgateway.domain.model;

import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class ExpiryDateTest {
    // Fixed "now" = 2026-06-15
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-15T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void acceptsFutureMonthYear() {
        ExpiryDate expiry = ExpiryDate.of(4, 2027, clock);
        assertEquals(4, expiry.month());
        assertEquals(2027, expiry.year());
    }

    @Test
    void acceptsCurrentMonth() {
        assertDoesNotThrow(() -> ExpiryDate.of(6, 2026, clock));
    }

    @Test
    void rejectsPastMonth() {
        assertThrows(ValidationException.class, () -> ExpiryDate.of(5, 2026, clock));
    }

    @Test
    void rejectsMonthOutOfRange() {
        assertThrows(ValidationException.class, () -> ExpiryDate.of(0, 2027, clock));
        assertThrows(ValidationException.class, () -> ExpiryDate.of(13, 2027, clock));
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew test --tests ExpiryDateTest`
Expected: FAIL — `ExpiryDate` does not exist.

- [ ] **Step 3: Implement `ExpiryDate`**

```java
package com.paymentgateway.domain.model;

import java.time.Clock;
import java.time.YearMonth;

public record ExpiryDate(int month, int year) {
    public static ExpiryDate of(int month, int year, Clock clock) {
        if (month < 1 || month > 12) {
            throw new ValidationException("expiry month must be between 1 and 12");
        }
        YearMonth expiry = YearMonth.of(year, month);
        YearMonth current = YearMonth.now(clock);
        if (expiry.isBefore(current)) {
            throw new ValidationException("card expiry must be in the future");
        }
        return new ExpiryDate(month, year);
    }
}
```

- [ ] **Step 4: Run to verify pass**

Run: `./gradlew test --tests ExpiryDateTest`
Expected: PASS.

- [ ] **Step 5: Commit** (subject to git policy)

```bash
git add src/main/java/com/paymentgateway/domain/model/ExpiryDate.java src/test/java/com/paymentgateway/domain/model/ExpiryDateTest.java
git commit -m "feat: add ExpiryDate value object with future-date validation"
```

---

### Task 5: `Card` value object

**Files:**
- Create: `src/main/java/com/paymentgateway/domain/model/Card.java`
- Test: `src/test/java/com/paymentgateway/domain/model/CardTest.java`

**Interfaces:**
- Consumes: `ExpiryDate`, `ValidationException`.
- Produces: `Card.of(String number, String cvv, ExpiryDate) -> Card`, `String number()`, `String cvv()`, `ExpiryDate expiry()`, `String lastFour()`.

- [ ] **Step 1: Write failing tests**

```java
package com.paymentgateway.domain.model;

import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class CardTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-15T00:00:00Z"), ZoneOffset.UTC);
    private final ExpiryDate expiry = ExpiryDate.of(4, 2027, clock);

    @Test
    void acceptsValidCardAndExposesLastFour() {
        Card card = Card.of("2222405343248877", "123", expiry);
        assertEquals("8877", card.lastFour());
        assertEquals("2222405343248877", card.number());
        assertEquals("123", card.cvv());
    }

    @Test
    void rejectsShortOrLongNumber() {
        assertThrows(ValidationException.class, () -> Card.of("1234567890123", "123", expiry));   // 13
        assertThrows(ValidationException.class, () -> Card.of("12345678901234567890", "123", expiry)); // 20
    }

    @Test
    void rejectsNonNumericNumber() {
        assertThrows(ValidationException.class, () -> Card.of("222240534324887X", "123", expiry));
    }

    @Test
    void rejectsBadCvv() {
        assertThrows(ValidationException.class, () -> Card.of("2222405343248877", "12", expiry));   // 2
        assertThrows(ValidationException.class, () -> Card.of("2222405343248877", "12345", expiry)); // 5
        assertThrows(ValidationException.class, () -> Card.of("2222405343248877", "12a", expiry));
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew test --tests CardTest`
Expected: FAIL — `Card` does not exist.

- [ ] **Step 3: Implement `Card`**

```java
package com.paymentgateway.domain.model;

public record Card(String number, String cvv, ExpiryDate expiry) {
    public static Card of(String number, String cvv, ExpiryDate expiry) {
        if (number == null || !number.matches("\\d{14,19}")) {
            throw new ValidationException("card number must be 14-19 digits");
        }
        if (cvv == null || !cvv.matches("\\d{3,4}")) {
            throw new ValidationException("cvv must be 3-4 digits");
        }
        return new Card(number, cvv, expiry);
    }

    public String lastFour() {
        return number.substring(number.length() - 4);
    }
}
```

- [ ] **Step 4: Run to verify pass**

Run: `./gradlew test --tests CardTest`
Expected: PASS.

- [ ] **Step 5: Commit** (subject to git policy)

```bash
git add src/main/java/com/paymentgateway/domain/model/Card.java src/test/java/com/paymentgateway/domain/model/CardTest.java
git commit -m "feat: add Card value object with last-four extraction"
```

---

### Task 6: `PaymentStatus` and `Payment` lifecycle

**Files:**
- Create: `src/main/java/com/paymentgateway/domain/model/PaymentStatus.java`
- Create: `src/main/java/com/paymentgateway/domain/model/Payment.java`
- Test: `src/test/java/com/paymentgateway/domain/model/PaymentTest.java`

**Interfaces:**
- Consumes: `ExpiryDate`, `Money`.
- Produces: `PaymentStatus{PENDING, AUTHORIZED, DECLINED}` with `String displayName()`; `Payment.pending(String lastFour, ExpiryDate, Money) -> Payment`; instance `authorize(String code) -> Payment`, `decline() -> Payment`; accessors `UUID id()`, `PaymentStatus status()`, `String lastFour()`, `ExpiryDate expiry()`, `Money money()`, `String authorizationCode()`.

- [ ] **Step 1: Write failing tests**

```java
package com.paymentgateway.domain.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PaymentTest {
    private final Money money = Money.of(Currency.of("GBP"), 100);
    private final ExpiryDate expiry = new ExpiryDate(4, 2027);

    @Test
    void pendingHasIdAndPendingStatus() {
        Payment p = Payment.pending("8877", expiry, money);
        assertNotNull(p.id());
        assertEquals(PaymentStatus.PENDING, p.status());
        assertNull(p.authorizationCode());
    }

    @Test
    void authorizeKeepsIdSetsCodeAndStatus() {
        Payment p = Payment.pending("8877", expiry, money);
        Payment authorized = p.authorize("auth-123");
        assertEquals(p.id(), authorized.id());
        assertEquals(PaymentStatus.AUTHORIZED, authorized.status());
        assertEquals("auth-123", authorized.authorizationCode());
    }

    @Test
    void declineKeepsIdSetsStatusNoCode() {
        Payment p = Payment.pending("8877", expiry, money);
        Payment declined = p.decline();
        assertEquals(p.id(), declined.id());
        assertEquals(PaymentStatus.DECLINED, declined.status());
        assertNull(declined.authorizationCode());
    }

    @Test
    void displayNamesAreTitleCase() {
        assertEquals("Authorized", PaymentStatus.AUTHORIZED.displayName());
        assertEquals("Declined", PaymentStatus.DECLINED.displayName());
        assertEquals("Pending", PaymentStatus.PENDING.displayName());
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew test --tests PaymentTest`
Expected: FAIL — types do not exist.

- [ ] **Step 3: Implement `PaymentStatus`**

```java
package com.paymentgateway.domain.model;

public enum PaymentStatus {
    PENDING("Pending"),
    AUTHORIZED("Authorized"),
    DECLINED("Declined");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
```

- [ ] **Step 4: Implement `Payment`**

```java
package com.paymentgateway.domain.model;

import java.util.UUID;

public record Payment(
        UUID id,
        PaymentStatus status,
        String lastFour,
        ExpiryDate expiry,
        Money money,
        String authorizationCode) {

    public static Payment pending(String lastFour, ExpiryDate expiry, Money money) {
        return new Payment(UUID.randomUUID(), PaymentStatus.PENDING, lastFour, expiry, money, null);
    }

    public Payment authorize(String authorizationCode) {
        return new Payment(id, PaymentStatus.AUTHORIZED, lastFour, expiry, money, authorizationCode);
    }

    public Payment decline() {
        return new Payment(id, PaymentStatus.DECLINED, lastFour, expiry, money, null);
    }
}
```

- [ ] **Step 5: Run to verify pass**

Run: `./gradlew test --tests PaymentTest`
Expected: PASS.

- [ ] **Step 6: Commit** (subject to git policy)

```bash
git add src/main/java/com/paymentgateway/domain/model/PaymentStatus.java src/main/java/com/paymentgateway/domain/model/Payment.java src/test/java/com/paymentgateway/domain/model/PaymentTest.java
git commit -m "feat: add Payment lifecycle and PaymentStatus"
```

---

### Task 7: Ports, command, and supporting domain types

No behavior to TDD here — these are interfaces and simple carriers consumed by later tasks. The test that exercises them is `ProcessPaymentServiceTest` (Task 8).

**Files:**
- Create: `src/main/java/com/paymentgateway/domain/port/in/PaymentRequest.java`
- Create: `src/main/java/com/paymentgateway/domain/port/in/ProcessPaymentUseCase.java`
- Create: `src/main/java/com/paymentgateway/domain/port/in/GetPaymentUseCase.java`
- Create: `src/main/java/com/paymentgateway/domain/port/out/PaymentRepository.java`
- Create: `src/main/java/com/paymentgateway/domain/port/out/AcquiringBankClient.java`
- Create: `src/main/java/com/paymentgateway/domain/port/out/BankResult.java`
- Create: `src/main/java/com/paymentgateway/domain/model/CurrencyAllowList.java`
- Create: `src/main/java/com/paymentgateway/domain/model/BankUnavailableException.java`
- Create: `src/main/java/com/paymentgateway/domain/model/PaymentNotFoundException.java`

**Interfaces:**
- Produces all of the above for Tasks 8–14.

- [ ] **Step 1: Create `PaymentRequest`**

```java
package com.paymentgateway.domain.port.in;

public record PaymentRequest(
        String cardNumber,
        int expiryMonth,
        int expiryYear,
        String currency,
        long amount,
        String cvv) {
}
```

- [ ] **Step 2: Create the inbound ports**

```java
package com.paymentgateway.domain.port.in;

import com.paymentgateway.domain.model.Payment;

public interface ProcessPaymentUseCase {
    Payment process(PaymentRequest request);
}
```

```java
package com.paymentgateway.domain.port.in;

import com.paymentgateway.domain.model.Payment;
import java.util.UUID;

public interface GetPaymentUseCase {
    Payment getById(UUID id);
}
```

- [ ] **Step 3: Create the outbound ports and `BankResult`**

```java
package com.paymentgateway.domain.port.out;

import com.paymentgateway.domain.model.Payment;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findById(UUID id);
}
```

```java
package com.paymentgateway.domain.port.out;

import com.paymentgateway.domain.model.Card;
import com.paymentgateway.domain.model.Money;

public interface AcquiringBankClient {
    BankResult authorize(Card card, Money money);
}
```

```java
package com.paymentgateway.domain.port.out;

public record BankResult(boolean authorized, String authorizationCode) {
}
```

- [ ] **Step 4: Create `CurrencyAllowList`**

```java
package com.paymentgateway.domain.model;

import java.util.Set;

public record CurrencyAllowList(Set<String> allowed) {
    public CurrencyAllowList {
        allowed = Set.copyOf(allowed);
    }

    public boolean contains(Currency currency) {
        return allowed.contains(currency.code());
    }
}
```

- [ ] **Step 5: Create the domain exceptions**

```java
package com.paymentgateway.domain.model;

public class BankUnavailableException extends RuntimeException {
    public BankUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

```java
package com.paymentgateway.domain.model;

import java.util.UUID;

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(UUID id) {
        super("payment not found: " + id);
    }
}
```

- [ ] **Step 6: Compile**

Run: `./gradlew compileJava`
Expected: PASS.

- [ ] **Step 7: Commit** (subject to git policy)

```bash
git add src/main/java/com/paymentgateway/domain/port/ src/main/java/com/paymentgateway/domain/model/CurrencyAllowList.java src/main/java/com/paymentgateway/domain/model/BankUnavailableException.java src/main/java/com/paymentgateway/domain/model/PaymentNotFoundException.java
git commit -m "feat: add ports, PaymentRequest command, and supporting domain types"
```

---

### Task 8: `ProcessPaymentService` (validation aggregation + persist-first flow)

**Files:**
- Create: `src/main/java/com/paymentgateway/domain/service/ProcessPaymentService.java`
- Test: `src/test/java/com/paymentgateway/domain/service/ProcessPaymentServiceTest.java`

**Interfaces:**
- Consumes: `PaymentRepository`, `AcquiringBankClient`, `BankResult`, `CurrencyAllowList`, `Clock`, all domain model types, `PaymentRequest`.
- Produces: `ProcessPaymentService(PaymentRepository, AcquiringBankClient, CurrencyAllowList, Clock)` implementing `ProcessPaymentUseCase`.

- [ ] **Step 1: Write failing tests (hand-rolled fakes, no Mockito)**

```java
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
        assertEquals(PaymentStatus.DECLINED, saved.get(1).status());
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
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew test --tests ProcessPaymentServiceTest`
Expected: FAIL — `ProcessPaymentService` does not exist.

- [ ] **Step 3: Implement `ProcessPaymentService`**

```java
package com.paymentgateway.domain.service;

import com.paymentgateway.domain.model.*;
import com.paymentgateway.domain.port.in.PaymentRequest;
import com.paymentgateway.domain.port.in.ProcessPaymentUseCase;
import com.paymentgateway.domain.port.out.AcquiringBankClient;
import com.paymentgateway.domain.port.out.BankResult;
import com.paymentgateway.domain.port.out.PaymentRepository;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ProcessPaymentService implements ProcessPaymentUseCase {

    private final PaymentRepository repository;
    private final AcquiringBankClient bankClient;
    private final CurrencyAllowList allowList;
    private final Clock clock;

    public ProcessPaymentService(PaymentRepository repository, AcquiringBankClient bankClient,
                                 CurrencyAllowList allowList, Clock clock) {
        this.repository = repository;
        this.bankClient = bankClient;
        this.allowList = allowList;
        this.clock = clock;
    }

    @Override
    public Payment process(PaymentRequest request) {
        List<String> errors = new ArrayList<>();

        Currency currency = collect(errors, () -> Currency.of(request.currency()));
        ExpiryDate expiry = collect(errors, () -> ExpiryDate.of(request.expiryMonth(), request.expiryYear(), clock));
        Card card = collect(errors, () -> Card.of(request.cardNumber(), request.cvv(), expiry));
        Money money = currency == null ? null : collect(errors, () -> Money.of(currency, request.amount()));

        if (currency != null && !allowList.contains(currency)) {
            errors.add("currency " + request.currency() + " is not supported");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        Payment payment = repository.save(Payment.pending(card.lastFour(), expiry, money));
        BankResult result = bankClient.authorize(card, money);
        Payment finalized = result.authorized()
                ? payment.authorize(result.authorizationCode())
                : payment.decline();
        return repository.save(finalized);
    }

    private <T> T collect(List<String> errors, Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (ValidationException e) {
            errors.addAll(e.errors());
            return null;
        }
    }
}
```

- [ ] **Step 4: Run to verify pass**

Run: `./gradlew test --tests ProcessPaymentServiceTest`
Expected: PASS.

- [ ] **Step 5: Commit** (subject to git policy)

```bash
git add src/main/java/com/paymentgateway/domain/service/ProcessPaymentService.java src/test/java/com/paymentgateway/domain/service/ProcessPaymentServiceTest.java
git commit -m "feat: add ProcessPaymentService with persist-first flow and error aggregation"
```

---

### Task 9: `GetPaymentService`

**Files:**
- Create: `src/main/java/com/paymentgateway/domain/service/GetPaymentService.java`
- Test: `src/test/java/com/paymentgateway/domain/service/GetPaymentServiceTest.java`

**Interfaces:**
- Consumes: `PaymentRepository`, `PaymentNotFoundException`.
- Produces: `GetPaymentService(PaymentRepository)` implementing `GetPaymentUseCase`.

- [ ] **Step 1: Write failing tests**

```java
package com.paymentgateway.domain.service;

import com.paymentgateway.domain.model.*;
import com.paymentgateway.domain.port.out.PaymentRepository;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GetPaymentServiceTest {

    private final Map<UUID, Payment> store = new HashMap<>();
    private final PaymentRepository repository = new PaymentRepository() {
        public Payment save(Payment p) { store.put(p.id(), p); return p; }
        public Optional<Payment> findById(UUID id) { return Optional.ofNullable(store.get(id)); }
    };

    @Test
    void returnsStoredPayment() {
        Payment p = Payment.pending("8877", new ExpiryDate(4, 2027), Money.of(Currency.of("GBP"), 100));
        repository.save(p);
        var service = new GetPaymentService(repository);
        assertEquals(p.id(), service.getById(p.id()).id());
    }

    @Test
    void throwsWhenMissing() {
        var service = new GetPaymentService(repository);
        assertThrows(PaymentNotFoundException.class, () -> service.getById(UUID.randomUUID()));
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew test --tests GetPaymentServiceTest`
Expected: FAIL.

- [ ] **Step 3: Implement `GetPaymentService`**

```java
package com.paymentgateway.domain.service;

import com.paymentgateway.domain.model.Payment;
import com.paymentgateway.domain.model.PaymentNotFoundException;
import com.paymentgateway.domain.port.in.GetPaymentUseCase;
import com.paymentgateway.domain.port.out.PaymentRepository;

import java.util.UUID;

public class GetPaymentService implements GetPaymentUseCase {

    private final PaymentRepository repository;

    public GetPaymentService(PaymentRepository repository) {
        this.repository = repository;
    }

    @Override
    public Payment getById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new PaymentNotFoundException(id));
    }
}
```

- [ ] **Step 4: Run to verify pass**

Run: `./gradlew test --tests GetPaymentServiceTest`
Expected: PASS.

- [ ] **Step 5: Commit** (subject to git policy)

```bash
git add src/main/java/com/paymentgateway/domain/service/GetPaymentService.java src/test/java/com/paymentgateway/domain/service/GetPaymentServiceTest.java
git commit -m "feat: add GetPaymentService"
```

---

### Task 10: `InMemoryPaymentRepository` adapter

**Files:**
- Create: `src/main/java/com/paymentgateway/adapter/out/persistence/InMemoryPaymentRepository.java`
- Test: `src/test/java/com/paymentgateway/adapter/out/persistence/InMemoryPaymentRepositoryTest.java`

**Interfaces:**
- Consumes: `PaymentRepository`, `Payment`.
- Produces: `InMemoryPaymentRepository implements PaymentRepository` (a `@Repository` bean, upsert by id).

- [ ] **Step 1: Write failing tests**

```java
package com.paymentgateway.adapter.out.persistence;

import com.paymentgateway.domain.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryPaymentRepositoryTest {

    private final InMemoryPaymentRepository repository = new InMemoryPaymentRepository();
    private final Payment pending = Payment.pending("8877", new ExpiryDate(4, 2027), Money.of(Currency.of("GBP"), 100));

    @Test
    void savesAndFindsById() {
        repository.save(pending);
        assertTrue(repository.findById(pending.id()).isPresent());
    }

    @Test
    void saveUpsertsById() {
        repository.save(pending);
        repository.save(pending.authorize("auth-1"));
        assertEquals(PaymentStatus.AUTHORIZED, repository.findById(pending.id()).orElseThrow().status());
    }

    @Test
    void findByUnknownIdIsEmpty() {
        assertTrue(repository.findById(java.util.UUID.randomUUID()).isEmpty());
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew test --tests InMemoryPaymentRepositoryTest`
Expected: FAIL.

- [ ] **Step 3: Implement `InMemoryPaymentRepository`**

```java
package com.paymentgateway.adapter.out.persistence;

import com.paymentgateway.domain.model.Payment;
import com.paymentgateway.domain.port.out.PaymentRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryPaymentRepository implements PaymentRepository {

    private final ConcurrentHashMap<UUID, Payment> store = new ConcurrentHashMap<>();

    @Override
    public Payment save(Payment payment) {
        store.put(payment.id(), payment);
        return payment;
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }
}
```

- [ ] **Step 4: Run to verify pass**

Run: `./gradlew test --tests InMemoryPaymentRepositoryTest`
Expected: PASS.

- [ ] **Step 5: Commit** (subject to git policy)

```bash
git add src/main/java/com/paymentgateway/adapter/out/persistence/ src/test/java/com/paymentgateway/adapter/out/persistence/
git commit -m "feat: add in-memory payment repository"
```

---

### Task 11: Bank adapter (`RestClientAcquiringBankClient`)

**Files:**
- Create: `src/main/java/com/paymentgateway/adapter/out/bank/BankPaymentRequest.java`
- Create: `src/main/java/com/paymentgateway/adapter/out/bank/BankPaymentResponse.java`
- Create: `src/main/java/com/paymentgateway/adapter/out/bank/RestClientAcquiringBankClient.java`
- Test: `src/integrationTest/java/com/paymentgateway/adapter/out/bank/RestClientAcquiringBankClientTest.java`

**Interfaces:**
- Consumes: `AcquiringBankClient`, `BankResult`, `Card`, `Money`, `BankUnavailableException`, Spring `RestClient`.
- Produces: `RestClientAcquiringBankClient(RestClient)` — `RestClient` is configured (base URL + snake_case ObjectMapper) by the config in Task 14.

- [ ] **Step 1: Write failing integration tests (MockWebServer)**

```java
package com.paymentgateway.adapter.out.bank;

import com.paymentgateway.domain.model.*;
import com.paymentgateway.domain.port.out.BankResult;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class RestClientAcquiringBankClientTest {

    private MockWebServer server;
    private RestClientAcquiringBankClient client;

    private final Card card = Card.of("2222405343248877", "123", new ExpiryDate(4, 2027));
    private final Money money = Money.of(Currency.of("GBP"), 100);

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        RestClient restClient = RestClient.builder().baseUrl(server.url("/").toString()).build();
        client = new RestClientAcquiringBankClient(restClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void mapsAuthorizedResponse() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"authorized\":true,\"authorization_code\":\"abc-123\"}"));

        BankResult result = client.authorize(card, money);

        assertTrue(result.authorized());
        assertEquals("abc-123", result.authorizationCode());
    }

    @Test
    void mapsDeclinedResponse() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"authorized\":false}"));

        assertFalse(client.authorize(card, money).authorized());
    }

    @Test
    void throwsBankUnavailableOn503() {
        server.enqueue(new MockResponse().setResponseCode(503));
        assertThrows(BankUnavailableException.class, () -> client.authorize(card, money));
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew integrationTest --tests RestClientAcquiringBankClientTest`
Expected: FAIL — types do not exist.

- [ ] **Step 3: Implement the DTOs**

```java
package com.paymentgateway.adapter.out.bank;

// Serialized with the app's global SNAKE_CASE strategy:
// cardNumber -> card_number, expiryDate -> expiry_date.
public record BankPaymentRequest(
        String cardNumber,
        String expiryDate,
        String currency,
        long amount,
        String cvv) {
}
```

```java
package com.paymentgateway.adapter.out.bank;

public record BankPaymentResponse(boolean authorized, String authorizationCode) {
}
```

- [ ] **Step 4: Implement `RestClientAcquiringBankClient`**

```java
package com.paymentgateway.adapter.out.bank;

import com.paymentgateway.domain.model.BankUnavailableException;
import com.paymentgateway.domain.model.Card;
import com.paymentgateway.domain.model.Money;
import com.paymentgateway.domain.port.out.AcquiringBankClient;
import com.paymentgateway.domain.port.out.BankResult;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class RestClientAcquiringBankClient implements AcquiringBankClient {

    private final RestClient restClient;

    public RestClientAcquiringBankClient(RestClient bankRestClient) {
        this.restClient = bankRestClient;
    }

    @Override
    public BankResult authorize(Card card, Money money) {
        BankPaymentRequest body = new BankPaymentRequest(
                card.number(),
                formatExpiry(card),
                money.currency().code(),
                money.amount(),
                card.cvv());
        try {
            BankPaymentResponse response = restClient.post()
                    .uri("/payments")
                    .body(body)
                    .retrieve()
                    .body(BankPaymentResponse.class);
            return new BankResult(response.authorized(), response.authorizationCode());
        } catch (RestClientException e) {
            throw new BankUnavailableException("acquiring bank unavailable", e);
        }
    }

    private String formatExpiry(Card card) {
        return String.format("%02d/%d", card.expiry().month(), card.expiry().year());
    }
}
```

Note: `.retrieve()` throws `RestClientException` subclasses on 4xx/5xx and on connection failures (`ResourceAccessException`), so all bank failures map to `BankUnavailableException`. The constructor parameter is named `bankRestClient` to match the qualifier bean defined in Task 14.

- [ ] **Step 5: Run to verify pass**

Run: `./gradlew integrationTest --tests RestClientAcquiringBankClientTest`
Expected: PASS.

- [ ] **Step 6: Commit** (subject to git policy)

```bash
git add src/main/java/com/paymentgateway/adapter/out/bank/ src/integrationTest/java/com/paymentgateway/adapter/out/bank/
git commit -m "feat: add RestClient acquiring bank adapter"
```

---

### Task 12: Web DTOs and mapper

**Files:**
- Create: `src/main/java/com/paymentgateway/adapter/in/web/PaymentRequestDto.java`
- Create: `src/main/java/com/paymentgateway/adapter/in/web/PaymentResponseDto.java`
- Create: `src/main/java/com/paymentgateway/adapter/in/web/PaymentWebMapper.java`
- Test: `src/test/java/com/paymentgateway/adapter/in/web/PaymentWebMapperTest.java`

**Interfaces:**
- Consumes: `PaymentRequest`, `Payment`.
- Produces: `PaymentWebMapper` with `PaymentRequest toCommand(PaymentRequestDto)` and `PaymentResponseDto toResponse(Payment)`.

- [ ] **Step 1: Write failing tests**

```java
package com.paymentgateway.adapter.in.web;

import com.paymentgateway.domain.model.*;
import com.paymentgateway.domain.port.in.PaymentRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentWebMapperTest {

    private final PaymentWebMapper mapper = new PaymentWebMapper();

    @Test
    void mapsDtoToCommand() {
        PaymentRequestDto dto = new PaymentRequestDto("2222405343248877", 4, 2027, "GBP", 100, "123");
        PaymentRequest command = mapper.toCommand(dto);
        assertEquals("2222405343248877", command.cardNumber());
        assertEquals(4, command.expiryMonth());
        assertEquals(100, command.amount());
        assertEquals("123", command.cvv());
    }

    @Test
    void mapsAuthorizedPaymentToResponseWithDisplayStatus() {
        Payment payment = Payment.pending("8877", new ExpiryDate(4, 2027), Money.of(Currency.of("GBP"), 100))
                .authorize("auth-9");
        PaymentResponseDto response = mapper.toResponse(payment);
        assertEquals(payment.id(), response.id());
        assertEquals("Authorized", response.status());
        assertEquals("8877", response.lastFour());
        assertEquals(4, response.expiryMonth());
        assertEquals(2027, response.expiryYear());
        assertEquals("GBP", response.currency());
        assertEquals(100, response.amount());
        assertEquals("auth-9", response.authorizationCode());
    }

    @Test
    void pendingResponseHasNoAuthorizationCode() {
        Payment payment = Payment.pending("8877", new ExpiryDate(4, 2027), Money.of(Currency.of("GBP"), 100));
        assertNull(mapper.toResponse(payment).authorizationCode());
        assertEquals("Pending", mapper.toResponse(payment).status());
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew test --tests PaymentWebMapperTest`
Expected: FAIL.

- [ ] **Step 3: Implement the DTOs**

```java
package com.paymentgateway.adapter.in.web;

// JSON (global SNAKE_CASE): card_number, expiry_month, expiry_year, currency, amount, cvv
public record PaymentRequestDto(
        String cardNumber,
        int expiryMonth,
        int expiryYear,
        String currency,
        long amount,
        String cvv) {
}
```

```java
package com.paymentgateway.adapter.in.web;

import java.util.UUID;

// JSON (global SNAKE_CASE, nulls omitted): id, status, last_four, expiry_month,
// expiry_year, currency, amount, authorization_code
public record PaymentResponseDto(
        UUID id,
        String status,
        String lastFour,
        int expiryMonth,
        int expiryYear,
        String currency,
        long amount,
        String authorizationCode) {
}
```

- [ ] **Step 4: Implement `PaymentWebMapper`**

```java
package com.paymentgateway.adapter.in.web;

import com.paymentgateway.domain.model.Payment;
import com.paymentgateway.domain.port.in.PaymentRequest;
import org.springframework.stereotype.Component;

@Component
public class PaymentWebMapper {

    public PaymentRequest toCommand(PaymentRequestDto dto) {
        return new PaymentRequest(
                dto.cardNumber(),
                dto.expiryMonth(),
                dto.expiryYear(),
                dto.currency(),
                dto.amount(),
                dto.cvv());
    }

    public PaymentResponseDto toResponse(Payment payment) {
        return new PaymentResponseDto(
                payment.id(),
                payment.status().displayName(),
                payment.lastFour(),
                payment.expiry().month(),
                payment.expiry().year(),
                payment.money().currency().code(),
                payment.money().amount(),
                payment.authorizationCode());
    }
}
```

- [ ] **Step 5: Run to verify pass**

Run: `./gradlew test --tests PaymentWebMapperTest`
Expected: PASS.

- [ ] **Step 6: Commit** (subject to git policy)

```bash
git add src/main/java/com/paymentgateway/adapter/in/web/Payment*.java src/test/java/com/paymentgateway/adapter/in/web/PaymentWebMapperTest.java
git commit -m "feat: add web DTOs and payment mapper"
```

---

### Task 13: `PaymentController` and error handling

**Files:**
- Create: `src/main/java/com/paymentgateway/adapter/in/web/PaymentController.java`
- Create: `src/main/java/com/paymentgateway/adapter/in/web/ValidationErrorResponse.java`
- Create: `src/main/java/com/paymentgateway/adapter/in/web/ErrorResponse.java`
- Create: `src/main/java/com/paymentgateway/adapter/in/web/PaymentExceptionHandler.java`
- Test: `src/integrationTest/java/com/paymentgateway/adapter/in/web/PaymentControllerTest.java`

**Interfaces:**
- Consumes: `ProcessPaymentUseCase`, `GetPaymentUseCase`, `PaymentWebMapper`, the domain exceptions.
- Produces: REST endpoints `POST /payments`, `GET /payments/{id}` and the advice mapping exceptions to 400/502/404.

- [ ] **Step 1: Write failing `@WebMvcTest`**

```java
package com.paymentgateway.adapter.in.web;

import com.paymentgateway.domain.model.*;
import com.paymentgateway.domain.port.in.GetPaymentUseCase;
import com.paymentgateway.domain.port.in.ProcessPaymentUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@Import({PaymentWebMapper.class, PaymentExceptionHandler.class})
class PaymentControllerTest {

    @Autowired MockMvc mvc;
    @MockBean ProcessPaymentUseCase processPaymentUseCase;
    @MockBean GetPaymentUseCase getPaymentUseCase;

    private final String validBody = """
        {"card_number":"2222405343248877","expiry_month":4,"expiry_year":2027,
         "currency":"GBP","amount":100,"cvv":"123"}
        """;

    @Test
    void postReturns200AndAuthorizedBody() throws Exception {
        Payment authorized = Payment.pending("8877", new ExpiryDate(4, 2027), Money.of(Currency.of("GBP"), 100))
                .authorize("auth-1");
        when(processPaymentUseCase.process(any())).thenReturn(authorized);

        mvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON).content(validBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Authorized"))
                .andExpect(jsonPath("$.last_four").value("8877"))
                .andExpect(jsonPath("$.authorization_code").value("auth-1"))
                .andExpect(jsonPath("$.cvv").doesNotExist())
                .andExpect(jsonPath("$.card_number").doesNotExist());
    }

    @Test
    void validationExceptionReturns400RejectedWithErrors() throws Exception {
        when(processPaymentUseCase.process(any()))
                .thenThrow(new ValidationException(java.util.List.of("cvv must be 3-4 digits")));

        mvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON).content(validBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("Rejected"))
                .andExpect(jsonPath("$.errors[0]").value("cvv must be 3-4 digits"));
    }

    @Test
    void bankUnavailableReturns502() throws Exception {
        when(processPaymentUseCase.process(any()))
                .thenThrow(new BankUnavailableException("down", null));

        mvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON).content(validBody))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("acquiring bank unavailable"));
    }

    @Test
    void getReturns200ForKnownPayment() throws Exception {
        Payment p = Payment.pending("8877", new ExpiryDate(4, 2027), Money.of(Currency.of("GBP"), 100));
        when(getPaymentUseCase.getById(p.id())).thenReturn(p);

        mvc.perform(get("/payments/{id}", p.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Pending"));
    }

    @Test
    void getReturns404ForUnknownPayment() throws Exception {
        UUID id = UUID.randomUUID();
        when(getPaymentUseCase.getById(id)).thenThrow(new PaymentNotFoundException(id));

        mvc.perform(get("/payments/{id}", id)).andExpect(status().isNotFound());
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew integrationTest --tests PaymentControllerTest`
Expected: FAIL — controller/handler do not exist.

- [ ] **Step 3: Implement the error response records**

```java
package com.paymentgateway.adapter.in.web;

import java.util.List;

public record ValidationErrorResponse(String status, List<String> errors) {
}
```

```java
package com.paymentgateway.adapter.in.web;

public record ErrorResponse(String error) {
}
```

- [ ] **Step 4: Implement `PaymentController`**

```java
package com.paymentgateway.adapter.in.web;

import com.paymentgateway.domain.model.Payment;
import com.paymentgateway.domain.port.in.GetPaymentUseCase;
import com.paymentgateway.domain.port.in.ProcessPaymentUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final ProcessPaymentUseCase processPaymentUseCase;
    private final GetPaymentUseCase getPaymentUseCase;
    private final PaymentWebMapper mapper;

    public PaymentController(ProcessPaymentUseCase processPaymentUseCase,
                             GetPaymentUseCase getPaymentUseCase,
                             PaymentWebMapper mapper) {
        this.processPaymentUseCase = processPaymentUseCase;
        this.getPaymentUseCase = getPaymentUseCase;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<PaymentResponseDto> process(@RequestBody PaymentRequestDto request) {
        Payment payment = processPaymentUseCase.process(mapper.toCommand(request));
        return ResponseEntity.ok(mapper.toResponse(payment));
    }

    @GetMapping("/{id}")
    public PaymentResponseDto getById(@PathVariable UUID id) {
        return mapper.toResponse(getPaymentUseCase.getById(id));
    }
}
```

- [ ] **Step 5: Implement `PaymentExceptionHandler`**

```java
package com.paymentgateway.adapter.in.web;

import com.paymentgateway.domain.model.BankUnavailableException;
import com.paymentgateway.domain.model.PaymentNotFoundException;
import com.paymentgateway.domain.model.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PaymentExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(ValidationException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ValidationErrorResponse("Rejected", e.errors()));
    }

    @ExceptionHandler(BankUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleBankUnavailable(BankUnavailableException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse("acquiring bank unavailable"));
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(PaymentNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("payment not found"));
    }
}
```

- [ ] **Step 6: Run to verify pass**

Run: `./gradlew integrationTest --tests PaymentControllerTest`
Expected: PASS.

- [ ] **Step 7: Commit** (subject to git policy)

```bash
git add src/main/java/com/paymentgateway/adapter/in/web/ src/integrationTest/java/com/paymentgateway/adapter/in/web/
git commit -m "feat: add payment REST controller and error handling"
```

---

### Task 14: Configuration & bean wiring

**Files:**
- Create: `src/main/java/com/paymentgateway/config/CurrencyProperties.java`
- Create: `src/main/java/com/paymentgateway/config/BankProperties.java`
- Create: `src/main/java/com/paymentgateway/config/BeanConfiguration.java`
- Modify: `src/main/java/com/paymentgateway/PaymentGatewayApplication.java` (add `@ConfigurationPropertiesScan`)
- Test: covered by the Task 1 `contextLoads` test and the Task 15 end-to-end test.

**Interfaces:**
- Produces beans: `Clock`, `CurrencyAllowList`, a qualified `RestClient bankRestClient`, `ProcessPaymentService`, `GetPaymentService`.

- [ ] **Step 1: Implement the configuration properties**

```java
package com.paymentgateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties(prefix = "payment.currencies")
public record CurrencyProperties(List<String> allowed) {
}
```

```java
package com.paymentgateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment.bank")
public record BankProperties(String baseUrl) {
}
```

- [ ] **Step 2: Enable properties scanning on the application class**

Modify `PaymentGatewayApplication.java` — add the annotation:

```java
package com.paymentgateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PaymentGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentGatewayApplication.class, args);
    }
}
```

- [ ] **Step 3: Implement `BeanConfiguration`**

```java
package com.paymentgateway.config;

import com.paymentgateway.domain.model.CurrencyAllowList;
import com.paymentgateway.domain.port.out.AcquiringBankClient;
import com.paymentgateway.domain.port.out.PaymentRepository;
import com.paymentgateway.domain.service.GetPaymentService;
import com.paymentgateway.domain.service.ProcessPaymentService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.util.Set;

@Configuration
public class BeanConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public CurrencyAllowList currencyAllowList(CurrencyProperties properties) {
        return new CurrencyAllowList(Set.copyOf(properties.allowed()));
    }

    @Bean
    public RestClient bankRestClient(RestClient.Builder builder, BankProperties properties) {
        return builder.baseUrl(properties.baseUrl()).build();
    }

    @Bean
    public ProcessPaymentService processPaymentService(PaymentRepository repository,
                                                       AcquiringBankClient bankClient,
                                                       CurrencyAllowList allowList,
                                                       Clock clock) {
        return new ProcessPaymentService(repository, bankClient, allowList, clock);
    }

    @Bean
    public GetPaymentService getPaymentService(PaymentRepository repository) {
        return new GetPaymentService(repository);
    }
}
```

Note: `RestClient.Builder` is auto-configured by Spring Boot and wired with the snake_case `ObjectMapper`, so the bank DTOs serialize correctly. `ProcessPaymentService`/`GetPaymentService` implement the inbound ports, satisfying the controller's constructor dependencies.

- [ ] **Step 4: Run the full build**

Run: `./gradlew clean build`
Expected: PASS — `contextLoads` confirms the wiring resolves.

- [ ] **Step 5: Commit** (subject to git policy)

```bash
git add src/main/java/com/paymentgateway/config/ src/main/java/com/paymentgateway/PaymentGatewayApplication.java
git commit -m "feat: wire beans, properties, and bank RestClient"
```

---

### Task 15: End-to-end smoke test

**Files:**
- Test: `src/integrationTest/java/com/paymentgateway/PaymentFlowIntegrationTest.java`

**Interfaces:**
- Consumes: the whole wired app + a `MockWebServer` standing in for the bank, base URL injected via `@DynamicPropertySource`.

- [ ] **Step 1: Write the end-to-end test**

```java
package com.paymentgateway;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentFlowIntegrationTest {

    private static MockWebServer bank;

    @Autowired
    private TestRestTemplate rest;

    @BeforeAll
    static void startBank() throws IOException {
        bank = new MockWebServer();
        bank.start();
    }

    @AfterAll
    static void stopBank() throws IOException {
        bank.shutdown();
    }

    @DynamicPropertySource
    static void bankUrl(DynamicPropertyRegistry registry) {
        registry.add("payment.bank.base-url", () -> bank.url("/").toString());
    }

    @Test
    void authorizesThenRetrievesPayment() {
        bank.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"authorized\":true,\"authorization_code\":\"abc-1\"}"));

        Map<String, Object> body = Map.of(
                "card_number", "2222405343248877",
                "expiry_month", 4,
                "expiry_year", 2027,
                "currency", "GBP",
                "amount", 100,
                "cvv", "123");

        ResponseEntity<Map> created = rest.postForEntity("/payments", body, Map.class);
        assertEquals(HttpStatus.OK, created.getStatusCode());
        assertEquals("Authorized", created.getBody().get("status"));
        assertNull(created.getBody().get("cvv"));
        String id = (String) created.getBody().get("id");

        ResponseEntity<Map> fetched = rest.getForEntity("/payments/" + id, Map.class);
        assertEquals(HttpStatus.OK, fetched.getStatusCode());
        assertEquals(id, fetched.getBody().get("id"));
        assertEquals("8877", fetched.getBody().get("last_four"));
    }

    @Test
    void rejectsInvalidPaymentWith400() {
        Map<String, Object> body = Map.of(
                "card_number", "123",
                "expiry_month", 4,
                "expiry_year", 2027,
                "currency", "GBP",
                "amount", 100,
                "cvv", "1");

        ResponseEntity<Map> response = rest.postForEntity("/payments", body, Map.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Rejected", response.getBody().get("status"));
    }
}
```

- [ ] **Step 2: Run to verify**

Run: `./gradlew integrationTest --tests PaymentFlowIntegrationTest`
Expected: PASS.

- [ ] **Step 3: Run the whole suite**

Run: `./gradlew clean build`
Expected: PASS — all unit and integration tests green.

- [ ] **Step 4: Commit** (subject to git policy)

```bash
git add src/integrationTest/java/com/paymentgateway/PaymentFlowIntegrationTest.java
git commit -m "test: add end-to-end payment flow integration test"
```

---

### Task 16: ADRs and README

**Files:**
- Create: `docs/adr/0001-use-hexagonal-architecture.md`
- Create: `docs/adr/0002-persist-first-payment-lifecycle.md`
- Create: `docs/adr/0003-rejected-as-400-no-resource.md`
- Create: `docs/adr/0004-in-memory-repository.md`
- Create: `docs/adr/0005-separate-unit-and-integration-source-sets.md`
- Modify: `README.md` (run instructions: build, run, bank simulator via Docker, ports)

**Interfaces:** none (documentation).

- [ ] **Step 1: Write each ADR** using the `CLAUDE.md` structure (Title, Status: Accepted, Context, Decision, Consequences). Each captures one decision from the spec's "ADRs to record" list. Keep them a few short paragraphs; no placeholders.

- [ ] **Step 2: Write `README.md`** covering: prerequisites (Java 21), `./gradlew clean build`, `./gradlew bootRun` (app on `8090`), running the bank simulator via Docker (`docker-compose up`, on `8080`), and how to run unit vs integration tests.

- [ ] **Step 3: Final verification**

Run: `./gradlew clean build`
Expected: PASS.

- [ ] **Step 4: Commit** (subject to git policy)

```bash
git add docs/adr/ README.md
git commit -m "docs: add ADRs and README"
```

---

## Self-Review

**Spec coverage:**
- Process payment → Tasks 8, 12, 13. Retrieve payment → Tasks 9, 13. ✓
- Validation rules (card/expiry/currency/amount/cvv) → Tasks 2–5, 8. ✓
- Rejected = 400, no resource → Tasks 8 (throws before persist), 13 (advice). ✓
- Persist-first + Pending; 502 on bank-unavailable → Tasks 8, 11, 13. ✓
- Bank simulator integration, base URL from config → Tasks 11, 14. ✓
- CVV/full PAN never stored or returned → enforced by `Payment` (Task 6) and asserted in Tasks 12, 13, 15. ✓
- Allow-list ≤3 currencies → Tasks 7, 8, 14. ✓
- Split source sets, TDD → Task 1, all subsequent tasks. ✓
- ADRs → Task 16. ✓

**Placeholder scan:** No "TBD"/"add error handling"-style placeholders; every code step contains complete code. ✓

**Type consistency:** `ProcessPaymentService(repository, bankClient, allowList, clock)` ctor matches Tasks 8 and 14; `BankResult(authorized, authorizationCode)` consistent across Tasks 7, 8, 11; `PaymentStatus.displayName()` used in Tasks 6, 12; `RestClient bankRestClient` qualifier matches Tasks 11 and 14; `PaymentRequest` field order identical in Tasks 7, 8, 12. ✓

**Decision (assumption to verify during execution):** Spring Boot 3.3.x auto-configured `RestClient.Builder` uses the snake_case `ObjectMapper`; if a future version changes this, Task 14's `bankRestClient` bean must set the message converter explicitly. Noted for the executor.
