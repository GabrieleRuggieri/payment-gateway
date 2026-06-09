package com.finance.payment;

import com.finance.payment.api.dto.CreatePaymentRequest;
import com.finance.payment.api.dto.IdempotentResult;
import com.finance.payment.api.dto.PaymentResponse;
import com.finance.payment.domain.OutboxStatus;
import com.finance.payment.repository.PaymentOutboxRepository;
import com.finance.payment.repository.PaymentRepository;
import com.finance.payment.service.PaymentService;
import com.finance.payment.support.IntegrationTestProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for payment creation, outbox co-commitment and idempotency (Testcontainers).
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@Tag("integration")
class PaymentServiceIntegrationTest {

    @Container
    @SuppressWarnings("resource") // closed by the Testcontainers JUnit extension
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("payments_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    @SuppressWarnings("resource") // closed by the Testcontainers JUnit extension
    static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.8.0")
    );

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentOutboxRepository outboxRepository;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        IntegrationTestProperties.registerDatastores(registry, postgres, kafka);
        registry.add("payment.outbox.relay.enabled", () -> "false");
        registry.add("payment.security.enabled", () -> "false");
    }

    @Test
    void shouldCreatePaymentAndOutboxEventInSameTransaction() {
        CreatePaymentRequest request = sampleRequest();
        String key = "integration-key-" + UUID.randomUUID();

        IdempotentResult<PaymentResponse> result = paymentService.initiatePayment(request, key);

        assertThat(result.replayed()).isFalse();
        assertThat(paymentRepository.findById(result.value().id())).isPresent();
        assertThat(outboxRepository.findByAggregateId(result.value().id()))
                .hasSize(1)
                .first()
                .satisfies(outbox -> {
                    assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
                    assertThat(outbox.getEventType()).isEqualTo("PaymentInitiated");
                });
    }

    @Test
    void shouldReturnSameResponseForDuplicateIdempotencyKey() {
        CreatePaymentRequest request = sampleRequest();
        String key = "dup-key-" + UUID.randomUUID();

        IdempotentResult<PaymentResponse> first = paymentService.initiatePayment(request, key);
        IdempotentResult<PaymentResponse> second = paymentService.initiatePayment(request, key);

        assertThat(first.replayed()).isFalse();
        assertThat(second.replayed()).isTrue();
        assertThat(second.value().id()).isEqualTo(first.value().id());
        assertThat(paymentRepository.count()).isEqualTo(1);
        assertThat(outboxRepository.findByAggregateId(first.value().id())).hasSize(1);
    }

    private static CreatePaymentRequest sampleRequest() {
        return new CreatePaymentRequest(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                new BigDecimal("42.50"),
                "EUR",
                "Integration test payment",
                null
        );
    }
}
