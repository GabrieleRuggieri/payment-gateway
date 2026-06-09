package com.finance.payment;

import com.finance.payment.api.dto.CreatePaymentRequest;
import com.finance.payment.api.dto.IdempotentResult;
import com.finance.payment.api.dto.PaymentResponse;
import com.finance.payment.domain.OutboxStatus;
import com.finance.payment.repository.PaymentOutboxRepository;
import com.finance.payment.service.OutboxRelayService;
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
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@Tag("integration")
class OutboxRelayIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("payments_outbox_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    @SuppressWarnings("resource")
    static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.8.0")
    );

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentOutboxRepository outboxRepository;

    @Autowired
    private OutboxRelayService outboxRelayService;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        IntegrationTestProperties.registerDatastores(registry, postgres, kafka);
        registry.add("payment.outbox.relay.enabled", () -> "true");
        registry.add("payment.outbox.relay.fixed-delay-ms", () -> "300");
        registry.add("payment.security.enabled", () -> "false");
    }

    @Test
    void shouldPublishPendingOutboxEventToKafka() {
        CreatePaymentRequest request = new CreatePaymentRequest(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                new BigDecimal("12.50"),
                "EUR",
                "Outbox relay integration test",
                null
        );
        String key = "outbox-relay-" + UUID.randomUUID();

        IdempotentResult<PaymentResponse> result = paymentService.initiatePayment(request, key);
        UUID paymentId = result.value().id();

        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {
            outboxRelayService.relay();
            assertThat(outboxRepository.findByAggregateId(paymentId))
                    .first()
                    .satisfies(outbox -> assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PUBLISHED));
        });
    }
}
