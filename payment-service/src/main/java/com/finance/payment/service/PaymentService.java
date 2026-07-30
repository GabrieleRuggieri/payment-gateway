package com.finance.payment.service;

import com.finance.payment.api.dto.CreatePaymentRequest;
import com.finance.payment.api.dto.IdempotentResult;
import com.finance.payment.api.dto.PaymentResponse;
import com.finance.payment.common.event.PaymentEventType;
import com.finance.payment.common.exception.PaymentNotFoundException;
import com.finance.payment.common.kafka.TopicConstants;
import com.finance.payment.config.PaymentEventMapper;
import com.finance.payment.domain.Payment;
import com.finance.payment.domain.PaymentAuditEvent;
import com.finance.payment.domain.PaymentOutbox;
import com.finance.payment.domain.PaymentStatus;
import com.finance.payment.repository.PaymentAuditEventRepository;
import com.finance.payment.repository.PaymentOutboxRepository;
import com.finance.payment.repository.PaymentRepository;
import com.finance.payment.security.MerchantAccessGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestra l'aggregato pagamento: avvio, transizioni di stato saga, audit trail e scritture outbox.
 * <p>
 * Ogni cambio di stato da pubblicare scrive su {@code payment_outbox} nella stessa transazione (pattern outbox).
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
    private final MerchantAccessGuard merchantAccessGuard;
    private final PaymentEventMapper eventMapper;

    /**
     * Crea un pagamento in modo idempotente e accoda un evento outbox {@link PaymentEventType#PAYMENT_INITIATED}.
     */
    @Transactional
    public IdempotentResult<PaymentResponse> initiatePayment(CreatePaymentRequest request, String idempotencyKey) {
        merchantAccessGuard.assertMerchantMatches(request.merchantId());
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

    /** Aggiorna l'aggregato dopo autorizzazione riuscita e memorizza il riferimento processore. */
    @Transactional
    public void handleAuthorized(UUID paymentId, String authorizationCode) {
        Payment payment = findPaymentOrThrow(paymentId);
        PaymentStatus old = payment.getStatus();
        payment.authorize();
        if (authorizationCode != null && !authorizationCode.isBlank() && !"null".equals(authorizationCode)) {
            payment.putMetadata("processorPaymentIntentId", authorizationCode);
            payment.putMetadata("authorizationCode", authorizationCode);
        }

        saveOutboxEvent(paymentId, PaymentEventType.PAYMENT_AUTHORIZED, buildPayload(payment));
        appendAudit(payment, PaymentEventType.PAYMENT_AUTHORIZED.wireName(), old, PaymentStatus.AUTHORIZED, null);
        log.info("Payment authorized: id={} processorRef={}", paymentId, authorizationCode);
    }

    /** Compat: authorize senza codice processore. */
    @Transactional
    public void handleAuthorized(UUID paymentId) {
        handleAuthorized(paymentId, null);
    }

    /** Segna il pagamento come fallito in fase di autorizzazione. */
    @Transactional
    public void handleAuthorizationFailed(UUID paymentId, String reason) {
        Payment payment = findPaymentOrThrow(paymentId);
        PaymentStatus old = payment.getStatus();
        payment.fail(PaymentStatus.INITIATED);

        appendAudit(payment, PaymentEventType.AUTHORIZATION_FAILED.wireName(), old, PaymentStatus.FAILED, Map.of("reason", reason));
        log.warn("Authorization failed for payment {}: {}", paymentId, reason);
    }

    /** Aggiorna l'aggregato dopo capture riuscita. */
    @Transactional
    public void handleCaptured(UUID paymentId) {
        Payment payment = findPaymentOrThrow(paymentId);
        PaymentStatus old = payment.getStatus();
        payment.capture();

        saveOutboxEvent(paymentId, PaymentEventType.PAYMENT_CAPTURED, buildPayload(payment));
        appendAudit(payment, PaymentEventType.PAYMENT_CAPTURED.wireName(), old, PaymentStatus.CAPTURED, null);
        log.info("Payment captured: id={}", paymentId);
    }

    /** Segna il pagamento come fallito in fase di capture. */
    @Transactional
    public void handleCaptureFailed(UUID paymentId, String reason) {
        Payment payment = findPaymentOrThrow(paymentId);
        PaymentStatus old = payment.getStatus();
        payment.fail(PaymentStatus.AUTHORIZED);

        appendAudit(payment, PaymentEventType.CAPTURE_FAILED.wireName(), old, PaymentStatus.FAILED, Map.of("reason", reason));
        log.warn("Capture failed for payment {}: {}", paymentId, reason);
    }

    /** Aggiorna l'aggregato dopo settlement riuscito. */
    @Transactional
    public void handleSettled(UUID paymentId) {
        Payment payment = findPaymentOrThrow(paymentId);
        PaymentStatus old = payment.getStatus();
        payment.settle();

        saveOutboxEvent(paymentId, PaymentEventType.PAYMENT_SETTLED, buildPayload(payment));
        appendAudit(payment, PaymentEventType.PAYMENT_SETTLED.wireName(), old, PaymentStatus.SETTLED, null);
        log.info("Payment settled: id={}", paymentId);
    }

    /** Registra un fallimento di settlement mantenendo lo stato CAPTURED fino al rimborso. */
    @Transactional
    public void handleSettlementFailed(UUID paymentId, String reason) {
        Payment payment = findPaymentOrThrow(paymentId);
        if (payment.getStatus() != PaymentStatus.CAPTURED) {
            log.debug("Ignoring SETTLEMENT_FAILED for payment {} in status {}", paymentId, payment.getStatus());
            return;
        }

        // Keep CAPTURED until PAYMENT_REFUNDED — compensation runs asynchronously.
        appendAudit(payment, PaymentEventType.SETTLEMENT_FAILED.wireName(), PaymentStatus.CAPTURED,
                PaymentStatus.CAPTURED, Map.of("reason", reason));
        log.warn("Settlement failed for payment {}: {}", paymentId, reason);
    }

    /** Aggiorna l'aggregato dopo rimborso completato (da CAPTURED o SETTLED). */
    @Transactional
    public void handleRefunded(UUID paymentId) {
        Payment payment = findPaymentOrThrow(paymentId);
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            log.debug("Ignoring duplicate PAYMENT_REFUNDED for payment {}", paymentId);
            return;
        }
        PaymentStatus old = payment.getStatus();
        payment.refund();

        saveOutboxEvent(paymentId, PaymentEventType.PAYMENT_REFUNDED, buildPayload(payment));
        appendAudit(payment, PaymentEventType.PAYMENT_REFUNDED.wireName(), old, PaymentStatus.REFUNDED, null);
        log.info("Payment refunded: id={}", paymentId);
    }

    /** Segna il pagamento come DISPUTED (webhook Stripe charge.dispute.*). */
    @Transactional
    public void handleDisputed(UUID paymentId, String reason) {
        Payment payment = findPaymentOrThrow(paymentId);
        if (payment.getStatus() == PaymentStatus.DISPUTED) {
            log.debug("Ignoring duplicate PAYMENT_DISPUTED for payment {}", paymentId);
            return;
        }
        PaymentStatus old = payment.getStatus();
        payment.dispute();

        Map<String, Object> auditPayload = reason != null ? Map.of("reason", reason) : Map.of();
        saveOutboxEvent(paymentId, PaymentEventType.PAYMENT_DISPUTED, buildPayload(payment));
        appendAudit(payment, PaymentEventType.PAYMENT_DISPUTED.wireName(), old, PaymentStatus.DISPUTED, auditPayload);
        log.warn("Payment disputed: id={} reason={}", paymentId, reason);
    }

    /** Risolve un pagamento dal PaymentIntent Stripe salvato in metadata. */
    public Optional<Payment> findByProcessorPaymentIntentId(String paymentIntentId) {
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            return Optional.empty();
        }
        return paymentRepository.findByProcessorPaymentIntentId(paymentIntentId);
    }

    /** Recupera un pagamento verificando l'appartenenza al merchant autenticato. */
    public PaymentResponse getPayment(UUID paymentId) {
        Payment payment = findPaymentOrThrow(paymentId);
        merchantAccessGuard.assertOwns(payment);
        return PaymentResponse.from(payment);
    }

    private Payment findPaymentOrThrow(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
    }

    private void saveOutboxEvent(UUID paymentId, PaymentEventType eventType, Map<String, Object> payload) {
        // Envelope PaymentEvent completo in payload → compatibile con Debezium Outbox Event Router
        // (table.expand.json.payload=true) e con il relay polling.
        Map<String, Object> envelope = eventMapper.toOutboxEnvelope(eventType.wireName(), paymentId, payload);
        outboxRepository.save(PaymentOutbox.of(
                paymentId,
                eventType.wireName(),
                envelope,
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
        if (payment.getMetadata() != null) {
            Object paymentMethodId = payment.getMetadata().get("paymentMethodId");
            if (paymentMethodId != null) {
                payload.put("paymentMethodId", paymentMethodId.toString());
            }
        }
        return payload;
    }
}
