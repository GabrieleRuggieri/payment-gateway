package com.finance.payment.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Proprietà di configurazione per l'autenticazione API del payment-service. */
@Getter
@Setter
@ConfigurationProperties(prefix = "payment.security")
public class PaymentSecurityProperties {

    /** Se false, i controlli API key sono disabilitati (solo test di integrazione). */
    private boolean enabled = true;

    private String apiKeyHeader = "X-Api-Key";
}
