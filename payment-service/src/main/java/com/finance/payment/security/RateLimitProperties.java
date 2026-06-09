package com.finance.payment.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Proprietà di configurazione per il rate limiting API per merchant. */
@Getter
@Setter
@ConfigurationProperties(prefix = "payment.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private int requestsPerMinute = 120;
}
