package com.finance.payment.service;

import com.finance.payment.api.dto.CreatePaymentRequest;
import com.finance.payment.api.dto.IdempotentResult;
import com.finance.payment.api.dto.PaymentResponse;
import com.finance.payment.common.event.PaymentEventType;
import com.finance.payment.common.exception.PaymentNotFoundException;
import com.finance.payment.common.kafka.TopicConstants;
import com.finance.payment.domain.Payment;
import com.finance.payment.domain.PaymentAuditEvent;
import com.finance.payment.domain.PaymentOutbox;
import com.finance.payment.domain.PaymentStatus;
import com.finance.payment.repository.PaymentAuditEventRepository;
import com.finance.payment.repository.PaymentOutboxRepository;
import com.finance.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestrates the payment aggregate: initiation, saga state transitions, audit trail and outbox writes.
 * <p>
 * Every state change that must be published writes to {@code payment_outbox} in the same transaction (outbox pattern).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private static final String SERVICE_NAME = "payment-service";

    private final PaymentRepository paymentRepository;
    private final PaymentOutboxRepository outboxRepository;
    private final PaymentAuditEventRepository auditEventRepository;
    private final IdempotencyService idempotencyService;

    /**
     * Creates a payment idempotently and enqueues a {@link PaymentEventType#PAYMENT_INITIATED} outbox event.
     */
    @Transactional
    public IdempotentResult<PaymentResponse> initiatePayment(CreatePaymentRequest request, String idempotencyKey) {
        return idempotencyService.executeIdempotent(idempotencyKey, () -> {
            Payment payment = Payment.initiate(
                    idempotencyKey,
                    request.merchantId(),
                    request.amount(),
                    request.currency(),
                    request.description(),
                    request.metadata()
            );
            paymentRepository.save(payment);

            Map<String, Object> payload = buildPayload(payment);
            saveOutboxEvent(payment.getId(), PaymentEventType.PAYMENT_INITIATED, payload);
            appendAudit(payment, PaymentEventType.PAYMENT_INITIATED.wireName(), null, PaymentStatus.INITIATED, null);

            log.info("Payment initiated: id={}, idempotencyKey={}", payment.getId(), idempotencyKey);
            return PaymentResponse.from(payment);
        });
    }

    @Transactional
    public void handleAuthorized(UUID paymentId) {
        Payment payment = findPaymentOrThrow(paymentId);
        PaymentStatus old = payment.getStatus();
        payment.authorize();

        saveOutboxEvent(paymentId, PaymentEventType.PAYMENT_AUTHORIZED, buildPayload(payment));
        appendAudit(payment, PaymentEventType.PAYMENT_AUTHORIZED.wireName(), old, PaymentStatus.AUTHORIZED, null);
        log.info("Payment authorized: id={}", paymentId);
    }

    @Transactional
    public void handleAuthorizationFailed(UUID paymentId, String reason) {
        Payment payment = findPaymentOrThrow(paymentId);
        PaymentStatus old = payment.getStatus();
        payment.fail(PaymentStatus.INITIATED);

        appendAudit(payment, PaymentEventType.AUTHORIZATION_FAILED.wireName(), old, PaymentStatus.FAILED, Map.of("reason", reason));
        log.warn("Authorization failed for payment {}: {}", paymentId, reason);
    }

    @Transactional
    public void handleCaptured(UUID paymentId) {
        Payment payment = findPaymentOrThrow(paymentId);
        PaymentStatus old = payment.getStatus();
        payment.capture();

        saveOutboxEvent(paymentId, PaymentEventType.PAYMENT_CAPTURED, buildPayload(payment));
        appendAudit(payment, PaymentEventType.PAYMENT_CAPTURED.wireName(), old, PaymentStatus.CAPTURED, null);
        log.info("Payment captured: id={}", paymentId);
    }

    @Transactional
    public void handleCaptureFailed(UUID paymentId, String reason) {
        Payment payment = findPaymentOrThrow(paymentId);
        PaymentStatus old = payment.getStatus();
        payment.fail(PaymentStatus.AUTHORIZED);

        appendAudit(payment, PaymentEventType.CAPTURE_FAILED.wireName(), old, PaymentStatus.FAILED, Map.of("reason", reason));
        log.warn("Capture failed for payment {}: {}", paymentId, reason);
    }

    @Transactional
    public void handleSettled(UUID paymentId) {
        Payment payment = findPaymentOrThrow(paymentId);
        PaymentStatus old = payment.getStatus();
        payment.settle();

        saveOutboxEvent(paymentId, PaymentEventType.PAYMENT_SETTLED, buildPayload(payment));
        appendAudit(payment, PaymentEventType.PAYMENT_SETTLED.wireName(), old, PaymentStatus.SETTLED, null);
        log.info("Payment settled: id={}", paymentId);
    }

    @Transactional
    public void handleSettlementFailed(UUID paymentId, String reason) {
        Payment payment = findPaymentOrThrow(paymentId);
        PaymentStatus old = payment.getStatus();
        payment.fail(PaymentStatus.CAPTURED);

        appendAudit(payment, PaymentEventType.SETTLEMENT_FAILED.wireName(), old, PaymentStatus.FAILED, Map.of("reason", reason));
        log.warn("Settlement failed for payment {}: {}", paymentId, reason);
    }

    @Transactional
    public void handleRefunded(UUID paymentId) {
        Payment payment = findPaymentOrThrow(paymentId);
        PaymentStatus old = payment.getStatus();
        payment.refund();

        saveOutboxEvent(paymentId, PaymentEventType.PAYMENT_REFUNDED, buildPayload(payment));
        appendAudit(payment, PaymentEventType.PAYMENT_REFUNDED.wireName(), old, PaymentStatus.REFUNDED, null);
        log.info("Payment refunded: id={}", paymentId);
    }

    public PaymentResponse getPayment(UUID paymentId) {
        return PaymentResponse.from(findPaymentOrThrow(paymentId));
    }

    private Payment findPaymentOrThrow(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
    }

    private void saveOutboxEvent(UUID paymentId, PaymentEventType eventType, Map<String, Object> payload) {
        outboxRepository.save(PaymentOutbox.of(
                paymentId,
                eventType.wireName(),
                payload,
                TopicConstants.PAYMENT_EVENTS
        ));
    }

    private void appendAudit(
            Payment payment,
            String eventType,
            PaymentStatus oldStatus,
            PaymentStatus newStatus,
            Map<String, Object> payload) {

        auditEventRepository.save(PaymentAuditEvent.of(
                payment.getId(),
                eventType,
                oldStatus,
                newStatus,
                payload,
                SERVICE_NAME
        ));
    }

    private Map<String, Object> buildPayload(Payment payment) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("paymentId", payment.getId().toString());
        payload.put("merchantId", payment.getMerchantId().toString());
        payload.put("amount", payment.getAmount());
        payload.put("currency", payment.getCurrency());
        payload.put("status", payment.getStatus().name());
        if (payment.getDescription() != null) {
            payload.put("description", payment.getDescription());
        }
        return payload;
    }
}
