package com.finance.payment.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "payment.security")
public class PaymentSecurityProperties {

    /** When false, API key checks are skipped (integration tests only). */
    private boolean enabled = true;

    private String apiKeyHeader = "X-Api-Key";
}
