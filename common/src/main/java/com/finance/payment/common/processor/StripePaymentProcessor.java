package com.finance.payment.common.processor;

import com.stripe.StripeClient;
import com.stripe.exception.CardException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCancelParams;
import com.stripe.param.PaymentIntentCaptureParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Adapter Stripe (test o live) per authorize / capture / void / refund.
 *
 * <p>In test mode usare PaymentMethod predefiniti ({@code pm_card_visa}, {@code pm_card_chargeDeclined}, …)
 * oppure carte di prova nel Dashboard Stripe — non muovono fondi reali.
 */
public class StripePaymentProcessor implements PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentProcessor.class);

    private final StripeClient stripe;
    private final String defaultPaymentMethod;

    public StripePaymentProcessor(StripeClient stripe, String defaultPaymentMethod) {
        this.stripe = stripe;
        this.defaultPaymentMethod = defaultPaymentMethod;
    }

    @Override
    public ProcessorResult authorize(UUID paymentId, BigDecimal amount, String currency, String paymentMethodId) {
        String pm = (paymentMethodId == null || paymentMethodId.isBlank())
                ? defaultPaymentMethod
                : paymentMethodId;
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(toMinorUnits(amount))
                    .setCurrency(currency.toLowerCase())
                    .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL)
                    .setConfirm(true)
                    .setPaymentMethod(pm)
                    .addPaymentMethodType("card")
                    .setConfirmationMethod(PaymentIntentCreateParams.ConfirmationMethod.AUTOMATIC)
                    .putMetadata("paymentId", paymentId.toString())
                    .setDescription("payment-gateway " + paymentId)
                    // Evita redirect 3DS nei test server-side
                    .setReturnUrl("https://localhost/stripe-return")
                    .build();

            PaymentIntent intent = stripe.paymentIntents().create(params);
            log.info("Stripe authorize payment={} pi={} status={}", paymentId, intent.getId(), intent.getStatus());

            if ("requires_capture".equals(intent.getStatus())) {
                return ProcessorResult.ok(intent.getId());
            }
            if ("succeeded".equals(intent.getStatus())) {
                // Alcuni PM catturano subito — trattiamo l'id come autorizzazione già catturabile/settlabile
                return ProcessorResult.ok(intent.getId());
            }
            return ProcessorResult.fail("Unexpected PaymentIntent status: " + intent.getStatus());
        } catch (CardException e) {
            log.warn("Stripe card declined for payment {}: {}", paymentId, e.getMessage());
            return ProcessorResult.fail(e.getMessage());
        } catch (StripeException e) {
            log.error("Stripe authorize failed for payment {}: {}", paymentId, e.getMessage());
            return ProcessorResult.fail(e.getMessage());
        }
    }

    @Override
    public ProcessorResult voidAuthorization(UUID paymentId, String authorizationCode) {
        try {
            PaymentIntent canceled = stripe.paymentIntents().cancel(
                    authorizationCode,
                    PaymentIntentCancelParams.builder().build());
            log.info("Stripe void payment={} pi={} status={}", paymentId, canceled.getId(), canceled.getStatus());
            return ProcessorResult.ok(canceled.getId());
        } catch (StripeException e) {
            log.error("Stripe void failed for payment {}: {}", paymentId, e.getMessage());
            return ProcessorResult.fail(e.getMessage());
        }
    }

    @Override
    public ProcessorResult capture(UUID paymentId, BigDecimal amount, String currency, String authorizationCode) {
        try {
            PaymentIntent existing = stripe.paymentIntents().retrieve(authorizationCode);
            if ("succeeded".equals(existing.getStatus())) {
                return ProcessorResult.ok(existing.getId());
            }
            PaymentIntent captured = stripe.paymentIntents().capture(
                    authorizationCode,
                    PaymentIntentCaptureParams.builder()
                            .setAmountToCapture(toMinorUnits(amount))
                            .build());
            log.info("Stripe capture payment={} pi={} status={}", paymentId, captured.getId(), captured.getStatus());
            if ("succeeded".equals(captured.getStatus())) {
                return ProcessorResult.ok(captured.getId());
            }
            return ProcessorResult.fail("Capture status: " + captured.getStatus());
        } catch (StripeException e) {
            log.error("Stripe capture failed for payment {}: {}", paymentId, e.getMessage());
            return ProcessorResult.fail(e.getMessage());
        }
    }

    @Override
    public ProcessorResult settle(
            UUID paymentId, UUID merchantId, BigDecimal amount, String currency, String captureReference) {
        try {
            String piId = captureReference != null && captureReference.startsWith("pi_")
                    ? captureReference
                    : null;
            if (piId == null) {
                // Reference mock o assente: in modalità Stripe il settle richiede un PaymentIntent id
                return ProcessorResult.fail("Missing Stripe PaymentIntent reference for settlement");
            }
            PaymentIntent intent = stripe.paymentIntents().retrieve(piId);
            if (!"succeeded".equals(intent.getStatus())) {
                return ProcessorResult.fail("PaymentIntent not settled: status=" + intent.getStatus());
            }
            String ref = intent.getLatestCharge() != null ? intent.getLatestCharge() : intent.getId();
            log.info("Stripe settle verified payment={} charge/pi={}", paymentId, ref);
            return ProcessorResult.ok(ref);
        } catch (StripeException e) {
            log.error("Stripe settle failed for payment {}: {}", paymentId, e.getMessage());
            return ProcessorResult.fail(e.getMessage());
        }
    }

    @Override
    public ProcessorResult refund(UUID paymentId, BigDecimal amount, String currency, String captureOrPaymentIntentId) {
        try {
            RefundCreateParams.Builder builder = RefundCreateParams.builder()
                    .setAmount(toMinorUnits(amount));
            if (captureOrPaymentIntentId != null && captureOrPaymentIntentId.startsWith("pi_")) {
                builder.setPaymentIntent(captureOrPaymentIntentId);
            } else if (captureOrPaymentIntentId != null && captureOrPaymentIntentId.startsWith("ch_")) {
                builder.setCharge(captureOrPaymentIntentId);
            } else {
                return ProcessorResult.fail("Missing Stripe PaymentIntent/Charge for refund");
            }
            Refund refund = stripe.refunds().create(builder.build());
            log.info("Stripe refund payment={} refund={} status={}", paymentId, refund.getId(), refund.getStatus());
            return ProcessorResult.ok(refund.getId());
        } catch (StripeException e) {
            log.error("Stripe refund failed for payment {}: {}", paymentId, e.getMessage());
            return ProcessorResult.fail(e.getMessage());
        }
    }

    /** Converte importo decimale in minor units (centesimi per EUR/USD). */
    static long toMinorUnits(BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
