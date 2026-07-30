package com.finance.payment.common.processor;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurazione del processore di pagamento.
 *
 * <p>{@code provider=mock} (default) — soglie demo locali, nessuna chiamata esterna.
 * <p>{@code provider=stripe} — Stripe test/live keys; richiede {@code stripe.api-key}.
 */
@ConfigurationProperties(prefix = "payment.processor")
public class ProcessorProperties {

    /** {@code mock} oppure {@code stripe}. */
    private String provider = "mock";

    private final Stripe stripe = new Stripe();
    private final Mock mock = new Mock();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Stripe getStripe() {
        return stripe;
    }

    public Mock getMock() {
        return mock;
    }

    public boolean isStripe() {
        return "stripe".equalsIgnoreCase(provider);
    }

    public static class Stripe {
        /** Secret key (sk_test_… in sandbox). */
        private String apiKey = "";
        /** PaymentMethod di default in test: {@code pm_card_visa}. */
        private String defaultPaymentMethod = "pm_card_visa";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getDefaultPaymentMethod() {
            return defaultPaymentMethod;
        }

        public void setDefaultPaymentMethod(String defaultPaymentMethod) {
            this.defaultPaymentMethod = defaultPaymentMethod;
        }
    }

    public static class Mock {
        private String authFailAbove = "9999";
        private String settlementFailAbove = "4999.99";
        private String captureFailAbove = "8999";

        public String getAuthFailAbove() {
            return authFailAbove;
        }

        public void setAuthFailAbove(String authFailAbove) {
            this.authFailAbove = authFailAbove;
        }

        public String getSettlementFailAbove() {
            return settlementFailAbove;
        }

        public void setSettlementFailAbove(String settlementFailAbove) {
            this.settlementFailAbove = settlementFailAbove;
        }

        public String getCaptureFailAbove() {
            return captureFailAbove;
        }

        public void setCaptureFailAbove(String captureFailAbove) {
            this.captureFailAbove = captureFailAbove;
        }
    }
}
