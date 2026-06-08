package com.finance.payment.notification.kafka;

import tools.jackson.databind.ObjectMapper;
import com.finance.payment.common.event.PaymentEvent;
import com.finance.payment.common.kafka.TopicConstants;
import com.finance.payment.notification.service.WebhookNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Forwards saga status events to merchant webhooks (filtered inside {@link WebhookNotificationService}).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final WebhookNotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = TopicConstants.PAYMENT_EVENTS,
            groupId = "notification-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handle(ConsumerRecord<String, String> record) {
        try {
            PaymentEvent event = objectMapper.readValue(record.value(), PaymentEvent.class);
            notificationService.notifyMerchant(event);
        } catch (Exception e) {
            log.error("Notification consumer error partition={} offset={}: {}",
                    record.partition(), record.offset(), e.getMessage());
            throw new IllegalStateException(e);
        }
    }
}
