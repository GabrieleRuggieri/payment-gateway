package com.finance.payment.authorization.kafka;

import tools.jackson.databind.ObjectMapper;
import com.finance.payment.authorization.service.AuthorizationService;
import com.finance.payment.common.event.PaymentEvent;
import com.finance.payment.common.event.PaymentEventType;
import com.finance.payment.common.kafka.TopicConstants;
import com.finance.payment.common.saga.SagaEventDedupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthorizationEventConsumer {

    private static final String CONSUMER_GROUP = "authorization-service";

    private final AuthorizationService authorizationService;
    private final SagaEventDedupService dedupService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = TopicConstants.PAYMENT_EVENTS,
            groupId = CONSUMER_GROUP,
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handlePaymentEvent(ConsumerRecord<String, String> record) {
        try {
            PaymentEvent event = objectMapper.readValue(record.value(), PaymentEvent.class);
            UUID paymentId = resolvePaymentId(event);

            if (!dedupService.registerIfNew(CONSUMER_GROUP, paymentId, event.getEventType())) {
                log.debug("Skipping duplicate event {} for payment {}", event.getEventType(), paymentId);
                return;
            }

            PaymentEventType type = PaymentEventType.fromWireName(event.getEventType());
            switch (type) {
                case PAYMENT_INITIATED -> handleInitiated(event, paymentId);
                case CAPTURE_FAILED -> handleCaptureFailed(event, paymentId);
                default -> log.trace("Ignoring event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("Error processing event partition={} offset={}: {}",
                    record.partition(), record.offset(), e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    private void handleInitiated(PaymentEvent event, UUID paymentId) throws Exception {
        BigDecimal amount = new BigDecimal(event.getPayload().get("amount").toString());
        String currency = event.getPayload().get("currency").toString();

        var result = authorizationService.authorize(paymentId, amount, currency);

        Map<String, Object> payload = new HashMap<>(event.getPayload());
        PaymentEventType outcome = result.isSuccess()
                ? PaymentEventType.PAYMENT_AUTHORIZED
                : PaymentEventType.AUTHORIZATION_FAILED;

        if (result.isSuccess()) {
            payload.put("authorizationCode", result.getAuthorizationCode());
        } else {
            payload.put("reason", result.getFailureReason());
        }

        publish(outcome, paymentId, payload);
    }

    private void handleCaptureFailed(PaymentEvent event, UUID paymentId) {
        String authCode = String.valueOf(event.getPayload().get("authorizationCode"));
        authorizationService.voidAuthorization(paymentId, authCode);
    }

    private void publish(PaymentEventType type, UUID paymentId, Map<String, Object> payload) throws Exception {
        PaymentEvent out = PaymentEvent.of(type, paymentId, payload);
        kafkaTemplate.send(TopicConstants.PAYMENT_EVENTS, paymentId.toString(), objectMapper.writeValueAsString(out));
    }

    private UUID resolvePaymentId(PaymentEvent event) {
        return event.getPaymentId() != null
                ? event.getPaymentId()
                : UUID.fromString(event.getPayload().get("paymentId").toString());
    }
}
