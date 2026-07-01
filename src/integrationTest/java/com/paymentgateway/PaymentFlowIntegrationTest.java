package com.paymentgateway;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentFlowIntegrationTest {

    private static final MockWebServer bank = new MockWebServer();

    @LocalServerPort
    private int port;

    private final RestClient client = RestClient.create();

    @AfterAll
    static void stopBank() throws IOException {
        bank.shutdown();
    }

    @DynamicPropertySource
    static void bankUrl(DynamicPropertyRegistry registry) {
        registry.add("payment.bank.base-url", () -> bank.url("/").toString());
    }

    private ResponseEntity<Map> post(Map<String, Object> body, String idempotencyKey) {
        RestClient.RequestBodySpec spec = client.post()
                .uri("http://localhost:" + port + "/payments")
                .contentType(MediaType.APPLICATION_JSON);
        if (idempotencyKey != null) {
            spec = spec.header("Idempotency-Key", idempotencyKey);
        }
        return spec.body(body)
                .retrieve()
                .onStatus(status -> true, (req, res) -> { })
                .toEntity(Map.class);
    }

    private ResponseEntity<Map> get(String id) {
        return client.get()
                .uri("http://localhost:" + port + "/payments/" + id)
                .retrieve()
                .onStatus(status -> true, (req, res) -> { })
                .toEntity(Map.class);
    }

    private Map<String, Object> validBody() {
        return Map.of(
                "card_number", "2222405343248877",
                "expiry_month", 4,
                "expiry_year", 2027,
                "currency", "GBP",
                "amount", 100,
                "cvv", "123");
    }

    @Test
    void authorizesThenRetrievesPayment() {
        bank.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"authorized\":true,\"authorization_code\":\"abc-1\"}"));

        ResponseEntity<Map> created = post(validBody(), null);
        assertEquals(200, created.getStatusCode().value());
        assertNotNull(created.getBody(), "POST /payments body must not be null");
        assertEquals("Authorized", created.getBody().get("status"));
        assertNull(created.getBody().get("cvv"));
        String id = (String) created.getBody().get("id");

        ResponseEntity<Map> fetched = get(id);
        assertEquals(200, fetched.getStatusCode().value());
        assertNotNull(fetched.getBody(), "GET /payments/{id} body must not be null");
        assertEquals(id, fetched.getBody().get("id"));
        assertEquals("8877", fetched.getBody().get("last_four"));
    }

    @Test
    void sameIdempotencyKeyReplaysFirstPaymentWithoutCallingBankTwice() {
        bank.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"authorized\":true,\"authorization_code\":\"abc-2\"}"));

        int bankCallsBefore = bank.getRequestCount();
        ResponseEntity<Map> first = post(validBody(), "key-flow-1");
        ResponseEntity<Map> replay = post(validBody(), "key-flow-1");

        assertEquals(200, first.getStatusCode().value());
        assertEquals(200, replay.getStatusCode().value());
        assertNotNull(first.getBody());
        assertNotNull(replay.getBody());
        assertEquals(first.getBody().get("id"), replay.getBody().get("id"));
        assertEquals(bankCallsBefore + 1, bank.getRequestCount(), "bank must be called exactly once for a replayed key");
    }

    @Test
    void bankFailureReturns502() {
        bank.enqueue(new MockResponse().setResponseCode(503));

        ResponseEntity<Map> created = post(validBody(), null);
        assertEquals(502, created.getStatusCode().value());
        assertNotNull(created.getBody(), "502 body must not be null");
        assertEquals("acquiring bank unavailable", created.getBody().get("error"));
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

        ResponseEntity<Map> response = post(body, null);
        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody(), "rejected response body must not be null");
        assertEquals("Rejected", response.getBody().get("status"));
    }

    @Test
    void replayWhileInDoubtPendingReturns409WithoutCallingBankAgain() {
        bank.enqueue(new MockResponse().setResponseCode(503));

        int bankCallsBefore = bank.getRequestCount();
        ResponseEntity<Map> inDoubt = post(validBody(), "key-indoubt");
        assertEquals(502, inDoubt.getStatusCode().value());

        ResponseEntity<Map> replay = post(validBody(), "key-indoubt");
        assertEquals(409, replay.getStatusCode().value());
        assertNotNull(replay.getBody());
        assertEquals("payment is still being processed", replay.getBody().get("error"));
        assertEquals(bankCallsBefore + 1, bank.getRequestCount(), "bank must not be re-called for an in-doubt replay");
    }

    @Test
    void unknownPaymentReturns404() {
        ResponseEntity<Map> response = get("00000000-0000-0000-0000-000000000000");
        assertEquals(404, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("payment not found", response.getBody().get("error"));
    }
}
