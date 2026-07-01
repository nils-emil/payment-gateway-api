package com.paymentgateway.adapter.out.bank;

import com.paymentgateway.domain.support.exception.*;
import com.paymentgateway.domain.model.*;
import com.paymentgateway.domain.port.out.AuthorizationCommand;
import com.paymentgateway.domain.port.out.BankResult;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class RestClientAcquiringBankClientTest {

    private MockWebServer server;
    private RestClientAcquiringBankClient client;

    private final AuthorizationCommand command = new AuthorizationCommand("2222405343248877", 4, 2027, new Money(100, "GBP"), "123");

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

        BankResult result = client.authorize(command);

        assertTrue(result.authorized());
        assertEquals("abc-123", result.authorizationCode());
    }

    @Test
    void sendsSnakeCaseRequestBodyToPaymentsEndpoint() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"authorized\":true,\"authorization_code\":\"abc-123\"}"));

        client.authorize(command);

        RecordedRequest request = server.takeRequest();
        assertEquals("/payments", request.getPath());
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("\"card_number\":\"2222405343248877\""), body);
        assertTrue(body.contains("\"expiry_date\":\"04/2027\""), body);
        assertFalse(body.contains("cardNumber"), body);
    }

    @Test
    void mapsDeclinedResponse() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"authorized\":false}"));

        assertFalse(client.authorize(command).authorized());
    }

    @Test
    void malformedResponseIsBankUnavailableNotDeclined() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"authorization_code\":\"abc-123\"}"));

        assertThrows(BankUnavailableException.class, () -> client.authorize(command));
    }

    @Test
    void authorizedWithoutCodeIsBankUnavailable() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"authorized\":true}"));

        assertThrows(BankUnavailableException.class, () -> client.authorize(command));
    }

    @Test
    void authorizedWithBlankCodeIsBankUnavailable() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"authorized\":true,\"authorization_code\":\"\"}"));

        assertThrows(BankUnavailableException.class, () -> client.authorize(command));
    }

    @Test
    void emptyResponseBodyIsBankUnavailable() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setResponseCode(200));

        assertThrows(BankUnavailableException.class, () -> client.authorize(command));
    }

    @Test
    void throwsBankUnavailableOn503() {
        server.enqueue(new MockResponse().setResponseCode(503));
        assertThrows(BankUnavailableException.class, () -> client.authorize(command));
    }

    @Test
    void throwsBankUnavailableOn4xx() {
        server.enqueue(new MockResponse().setResponseCode(400));
        assertThrows(BankUnavailableException.class, () -> client.authorize(command));
    }

    @Test
    void readTimeoutMapsToBankUnavailable() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"authorized\":true,\"authorization_code\":\"abc-123\"}")
                .setBodyDelay(500, TimeUnit.MILLISECONDS));
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofMillis(50));
        RestClient timeoutRestClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(server.url("/").toString())
                .build();
        RestClientAcquiringBankClient timeoutClient = new RestClientAcquiringBankClient(timeoutRestClient);

        assertThrows(BankUnavailableException.class, () -> timeoutClient.authorize(command));
    }
}
