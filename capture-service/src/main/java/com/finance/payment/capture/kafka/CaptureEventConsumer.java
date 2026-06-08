package com.finance.payment.capture.kafka;

import tools.jackson.databind.ObjectMapper;
import com.finance.payment.capture.service.CaptureService;
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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CaptureEventConsumer {

    private static final String CONSUMER_GROUP = "capture-service";

    private final CaptureService captureService;
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
            if (!PaymentEventType.PAYMENT_AUTHORIZED.wireName().equals(event.getEventType())) {
                return;
            }

            UUID paymentId = resolvePaymentId(event);
            if (!dedupService.registerIfNew(CONSUMER_GROUP, paymentId, event.getEventType())) {
                log.debug("Skipping duplicate capture event for payment {}", paymentId);
                return;
            }

            BigDecimal amount = new BigDecimal(event.getPayload().get("amount").toString());
            String currency = event.getPayload().get("currency").toString();
            String authCode = String.valueOf(event.getPayload().get("authorizationCode"));

            var result = captureService.capture(paymentId, amount, currency, authCode);
            Map<String, Object> payload = new HashMap<>(event.getPayload());

            PaymentEventType outcome = result.isSuccess()
                    ? PaymentEventType.PAYMENT_CAPTURED
                    : PaymentEventType.CAPTURE_FAILED;

            if (!result.isSuccess()) {
                payload.put("reason", result.getFailureReason());
            } else {
                payload.put("captureReference", result.getCaptureReference());
            }

            PaymentEvent out = PaymentEvent.of(outcome, paymentId, payload);
            kafkaTemplate.send(TopicConstants.PAYMENT_EVENTS, paymentId.toString(), objectMapper.writeValueAsString(out));
        } catch (Exception e) {
            log.error("Capture consumer error: {}", e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    private UUID resolvePaymentId(PaymentEvent event) {
        return event.getPaymentId() != null
                ? event.getPaymentId()
                : UUID.fromString(event.getPayload().get("paymentId").toString());
    }
}
