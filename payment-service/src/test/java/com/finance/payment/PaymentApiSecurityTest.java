package com.finance.payment;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("payment.outbox.relay.enabled", () -> "false");
        registry.add("payment.security.enabled", () -> "true");
        registry.add("payment.rate-limit.enabled", () -> "false");
        registry.add("management.health.redis.enabled", () -> "false");
        registry.add("management.health.kafka.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowActuatorHealthWithoutApiKey() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
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
