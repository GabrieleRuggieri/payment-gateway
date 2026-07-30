package com.finance.payment.stripe;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configurazione webhook inbound Stripe (test mode consigliato in locale). */
@ConfigurationProperties(prefix = "payment.stripe")
public class StripeWebhookProperties {

    /** Secret endpoint ({@code whsec_...}) da Stripe CLI o Dashboard. */
    private String webhookSecret = "";

    /** Se false, l'endpoint risponde 503 (utile quando PROVIDER=mock). */
    private boolean webhooksEnabled = false;

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public boolean isWebhooksEnabled() {
        return webhooksEnabled;
    }

    public void setWebhooksEnabled(boolean webhooksEnabled) {
        this.webhooksEnabled = webhooksEnabled;
    }
}
