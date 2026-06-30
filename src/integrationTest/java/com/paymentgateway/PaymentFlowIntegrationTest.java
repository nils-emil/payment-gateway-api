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

    private static final MockWebServer bank = new MockWebServer();

    @Autowired
    private TestRestTemplate rest;

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
        assertNotNull(created.getBody(), "POST /payments body must not be null");
        assertEquals("Authorized", created.getBody().get("status"));
        assertNull(created.getBody().get("cvv"));
        String id = (String) created.getBody().get("id");

        ResponseEntity<Map> fetched = rest.getForEntity("/payments/" + id, Map.class);
        assertEquals(HttpStatus.OK, fetched.getStatusCode());
        assertNotNull(fetched.getBody(), "GET /payments/{id} body must not be null");
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
        assertNotNull(response.getBody(), "rejected response body must not be null");
        assertEquals("Rejected", response.getBody().get("status"));
    }
}
