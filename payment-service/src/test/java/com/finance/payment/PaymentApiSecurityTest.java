package com.finance.payment;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import com.finance.payment.support.IntegrationTestProperties;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test di integrazione per la sicurezza dell'API: health actuator senza chiave API, rifiuto delle richieste
 * non autenticate, blocco per merchant non corrispondente e accettazione con credenziali valide.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@Tag("integration")
class PaymentApiSecurityTest {

    private static final String DEMO_API_KEY = "pgw-demo-key-32chars-minimum!!";
    private static final String DEMO_MERCHANT = "550e8400-e29b-41d4-a716-446655440000";

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("payments_security_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    @SuppressWarnings("resource")
    static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.8.0")
    );

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        IntegrationTestProperties.registerDatastores(registry, postgres, kafka);
        registry.add("payment.outbox.relay.enabled", () -> "false");
        registry.add("payment.security.enabled", () -> "true");
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowActuatorHealthWithoutApiKey() throws Exception {
        // La sonda non deve richiedere X-Api-Key (healthcheck Docker); lo stato può essere 200 o 503 senza Redis
        int status = mockMvc.perform(get("/actuator/health"))
                .andReturn()
                .getResponse()
                .getStatus();
        assertThat(status).isNotEqualTo(401);
    }

    @Test
    void shouldRejectRequestWithoutApiKey() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "security-test-key-001")
                        .content(paymentBody(DEMO_MERCHANT)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectMerchantMismatch() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Api-Key", DEMO_API_KEY)
                        .header("Idempotency-Key", "security-test-key-002")
                        .content(paymentBody("00000000-0000-0000-0000-000000000001")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAcceptValidApiKeyAndMerchant() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Api-Key", DEMO_API_KEY)
                        .header("Idempotency-Key", "security-test-key-003")
                        .content(paymentBody(DEMO_MERCHANT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchantId").value(DEMO_MERCHANT));
    }

    private static String paymentBody(String merchantId) {
        return """
                {
                  "merchantId": "%s",
                  "amount": "10.00",
                  "currency": "EUR"
                }
                """.formatted(merchantId);
    }
}
