package com.finance.payment.service;

import com.finance.payment.api.dto.CreatePaymentRequest;
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

import java.util.Map;
import java.util.UUID;

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
     * TODO: Wire idempotency + outbox in single transaction (see README §6.3).
     * - Payment.initiate(...)
     * - save payment
     * - save PaymentOutbox PaymentInitiated event
     * - append PaymentAuditEvent
     */
    @Transactional
    public PaymentResponse initiatePayment(CreatePaymentRequest request, String idempotencyKey) {
        return idempotencyService.executeIdempotent(
                idempotencyKey,
                () -> {
                    Payment payment = Payment.initiate(
                            idempotencyKey,
                            request.merchantId(),
                            request.amount(),
                            request.currency(),
                            request.description(),
                            request.metadata()
                    );
                    paymentRepository.save(payment);

                    saveOutboxEvent(payment.getId(), PaymentEventType.PAYMENT_INITIATED, buildInitiatedPayload(payment));
                    appendAudit(payment, PaymentEventType.PAYMENT_INITIATED.wireName(), null, PaymentStatus.INITIATED, null);

                    log.info("Payment initiated: id={}, idempotencyKey={}", payment.getId(), idempotencyKey);
                    return PaymentResponse.from(payment);
                },
                PaymentResponse.class
        );
    }

    /**
     * TODO: Called by Kafka consumer when Authorization service publishes PaymentAuthorized.
     */
    @Transactional
    public void handleAuthorized(UUID paymentId) {
        Payment payment = findPaymentOrThrow(paymentId);
        PaymentStatus old = payment.getStatus();
        payment.authorize();

        saveOutboxEvent(paymentId, PaymentEventType.PAYMENT_AUTHORIZED, Map.of("paymentId", paymentId.toString()));
        appendAudit(payment, PaymentEventType.PAYMENT_AUTHORIZED.wireName(), old, PaymentStatus.AUTHORIZED, null);
    }

    /**
     * TODO: Called when authorization fails — no compensation needed.
     */
    @Transactional
    public void handleAuthorizationFailed(UUID paymentId, String reason) {
        Payment payment = findPaymentOrThrow(paymentId);
        PaymentStatus old = payment.getStatus();
        payment.fail(PaymentStatus.INITIATED);

        appendAudit(payment, PaymentEventType.AUTHORIZATION_FAILED.wireName(), old, PaymentStatus.FAILED, Map.of("reason", reason));
        log.warn("Authorization failed for payment {}: {}", paymentId, reason);
    }

    /**
     * TODO: Implement handleCaptured, handleCaptureFailed, handleSettled, handleSettlementFailed.
     */
    @Transactional
    public void handleCaptured(UUID paymentId) {
        throw new UnsupportedOperationException("TODO: implement capture transition + outbox");
    }

    @Transactional
    public void handleSettled(UUID paymentId) {
        throw new UnsupportedOperationException("TODO: implement settle transition + outbox");
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

    private Map<String, Object> buildInitiatedPayload(Payment payment) {
        return Map.of(
                "paymentId", payment.getId().toString(),
                "amount", payment.getAmount(),
                "currency", payment.getCurrency(),
                "merchantId", payment.getMerchantId().toString()
        );
    }
}
