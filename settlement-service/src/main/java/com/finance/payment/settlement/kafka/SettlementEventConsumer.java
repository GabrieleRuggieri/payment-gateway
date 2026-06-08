package com.finance.payment.settlement.kafka;

import tools.jackson.databind.ObjectMapper;
import com.finance.payment.common.event.PaymentEvent;
import com.finance.payment.common.event.PaymentEventType;
import com.finance.payment.common.kafka.TopicConstants;
import com.finance.payment.common.saga.SagaEventDedupService;
import com.finance.payment.settlement.service.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SettlementEventConsumer {

    private static final String CONSUMER_GROUP = "settlement-service";

    private final SettlementService settlementService;
    private final SagaEventDedupService dedupService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = TopicConstants.PAYMENT_EVENTS,
            groupId = CONSUMER_GROUP,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handle(ConsumerRecord<String, String> record) {
        try {
            PaymentEvent event = objectMapper.readValue(record.value(), PaymentEvent.class);
            PaymentEventType type = PaymentEventType.fromWireName(event.getEventType());

            if (type != PaymentEventType.PAYMENT_CAPTURED && type != PaymentEventType.SETTLEMENT_FAILED) {
                return;
            }

            UUID paymentId = resolvePaymentId(event);
            if (!dedupService.registerIfNew(CONSUMER_GROUP, paymentId, event.getEventType())) {
                log.debug("Skipping duplicate settlement event for payment {}", paymentId);
                return;
            }

            if (type == PaymentEventType.PAYMENT_CAPTURED) {
                handleCaptured(event, paymentId);
            } else {
                handleSettlementFailed(event, paymentId);
            }
        } catch (Exception e) {
            log.error("Settlement consumer error: {}", e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    private void handleCaptured(PaymentEvent event, UUID paymentId) throws Exception {
        UUID merchantId = UUID.fromString(event.getPayload().get("merchantId").toString());
        BigDecimal amount = new BigDecimal(event.getPayload().get("amount").toString());
        String currency = event.getPayload().get("currency").toString();

        var result = settlementService.settle(paymentId, merchantId, amount, currency);
        Map<String, Object> payload = new HashMap<>(event.getPayload());

        PaymentEventType outcome = result.isSuccess()
                ? PaymentEventType.PAYMENT_SETTLED
                : PaymentEventType.SETTLEMENT_FAILED;

        if (result.isSuccess()) {
            payload.put("settlementReference", result.getSettlementReference());
        } else {
            payload.put("reason", result.getFailureReason());
        }

        publish(outcome, paymentId, payload);
    }

    private void handleSettlementFailed(PaymentEvent event, UUID paymentId) {
        BigDecimal amount = new BigDecimal(event.getPayload().get("amount").toString());
        String currency = event.getPayload().get("currency").toString();
        settlementService.refund(paymentId, amount, currency);
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
