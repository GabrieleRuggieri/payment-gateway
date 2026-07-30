package com.finance.payment.common.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Processore demo con soglie su importo — nessuna chiamata di rete.
 * Utile per CI e per esercitare i path di fallimento della saga senza Stripe.
 */
public class MockPaymentProcessor implements PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentProcessor.class);

    private final BigDecimal authFailAbove;
    private final BigDecimal settlementFailAbove;
    private final BigDecimal captureFailAbove;

    public MockPaymentProcessor(ProcessorProperties properties) {
        this.authFailAbove = new BigDecimal(properties.getMock().getAuthFailAbove());
        this.settlementFailAbove = new BigDecimal(properties.getMock().getSettlementFailAbove());
        this.captureFailAbove = new BigDecimal(properties.getMock().getCaptureFailAbove());
    }

    @Override
    public ProcessorResult authorize(UUID paymentId, BigDecimal amount, String currency, String paymentMethodId) {
        log.debug("Mock authorize payment={} amount={} {} pm={}", paymentId, amount, currency, paymentMethodId);
        if (amount.compareTo(authFailAbove) > 0) {
            return ProcessorResult.fail("Limit exceeded");
        }
        return ProcessorResult.ok("AUTH-" + shortId(paymentId));
    }

    @Override
    public ProcessorResult voidAuthorization(UUID paymentId, String authorizationCode) {
        log.info("Mock void payment={} auth={}", paymentId, authorizationCode);
        return ProcessorResult.ok("VOID-" + shortId(paymentId));
    }

    @Override
    public ProcessorResult capture(UUID paymentId, BigDecimal amount, String currency, String authorizationCode) {
        log.info("Mock capture payment={} amount={} {} auth={}", paymentId, amount, currency, authorizationCode);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ProcessorResult.fail("Invalid capture amount");
        }
        if (amount.compareTo(captureFailAbove) > 0 && amount.compareTo(authFailAbove) <= 0) {
            return ProcessorResult.fail("Mock processor rejected capture");
        }
        return ProcessorResult.ok("CAP-" + shortId(paymentId));
    }

    @Override
    public ProcessorResult settle(
            UUID paymentId, UUID merchantId, BigDecimal amount, String currency, String captureReference) {
        log.info("Mock settle payment={} merchant={} amount={} {}", paymentId, merchantId, amount, currency);
        if (amount.compareTo(settlementFailAbove) > 0) {
            return ProcessorResult.fail("Acquirer rejected: amount exceeds settlement limit");
        }
        return ProcessorResult.ok("SET-" + shortId(paymentId));
    }

    @Override
    public ProcessorResult refund(UUID paymentId, BigDecimal amount, String currency, String captureOrPaymentIntentId) {
        log.info("Mock refund payment={} amount={} {}", paymentId, amount, currency);
        return ProcessorResult.ok("REF-" + shortId(paymentId));
    }

    private static String shortId(UUID paymentId) {
        return paymentId.toString().substring(0, 8).toUpperCase();
    }
}
