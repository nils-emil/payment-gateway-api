package com.paymentgateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiDocsIntegrationTest {

    @LocalServerPort
    private int port;

    private final RestClient client = RestClient.create();

    @Test
    void publishesOpenApiDocsForThePaymentsApi() {
        ResponseEntity<String> docs = client.get()
                .uri("http://localhost:" + port + "/v3/api-docs")
                .retrieve()
                .toEntity(String.class);

        assertEquals(200, docs.getStatusCode().value());
        assertNotNull(docs.getBody());
        assertTrue(docs.getBody().contains("\"/payments\""), "OpenAPI doc must describe the /payments endpoint");
        assertTrue(docs.getBody().contains("Payment Gateway API"), "OpenAPI doc must carry the configured API title");
    }

    @Test
    void servesSwaggerUi() {
        ResponseEntity<String> ui = client.get()
                .uri("http://localhost:" + port + "/swagger-ui/index.html")
                .retrieve()
                .toEntity(String.class);

        assertEquals(200, ui.getStatusCode().value());
    }
}
