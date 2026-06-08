package com.finance.payment.config;

import tools.jackson.databind.ObjectMapper;
import com.finance.payment.common.event.PaymentEvent;
import com.finance.payment.common.event.PaymentEventType;
import com.finance.payment.common.saga.SagaEventDedupService;
import com.finance.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Updates the payment aggregate when downstream saga steps complete.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentSagaConsumer {

    private static final String CONSUMER_GROUP = "payment-service-saga";

    private final PaymentService paymentService;
    private final SagaEventDedupService dedupService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${payment.kafka.topics.events:payment.events}",
            groupId = CONSUMER_GROUP,
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onPaymentEvent(ConsumerRecord<String, String> record) {
        try {
            PaymentEvent event = objectMapper.readValue(record.value(), PaymentEvent.class);
            UUID paymentId = resolvePaymentId(event);

            if (!dedupService.registerIfNew(CONSUMER_GROUP, paymentId, event.getEventType())) {
                log.debug("Skipping duplicate saga event {} for payment {}", event.getEventType(), paymentId);
                return;
            }

            PaymentEventType type = PaymentEventType.fromWireName(event.getEventType());
            String reason = String.valueOf(event.getPayload().getOrDefault("reason", "unknown"));

            switch (type) {
                case PAYMENT_AUTHORIZED -> paymentService.handleAuthorized(paymentId);
                case AUTHORIZATION_FAILED -> paymentService.handleAuthorizationFailed(paymentId, reason);
                case PAYMENT_CAPTURED -> paymentService.handleCaptured(paymentId);
                case CAPTURE_FAILED -> paymentService.handleCaptureFailed(paymentId, reason);
                case PAYMENT_SETTLED -> paymentService.handleSettled(paymentId);
                case SETTLEMENT_FAILED -> paymentService.handleSettlementFailed(paymentId, reason);
                case PAYMENT_REFUNDED -> paymentService.handleRefunded(paymentId);
                default -> log.trace("Ignoring event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("Error processing saga event partition={} offset={}: {}",
                    record.partition(), record.offset(), e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    private UUID resolvePaymentId(PaymentEvent event) {
        return event.getPaymentId() != null
                ? event.getPaymentId()
                : UUID.fromString(String.valueOf(event.getPayload().get("paymentId")));
    }
}
