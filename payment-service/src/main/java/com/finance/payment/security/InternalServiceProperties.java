package com.finance.payment.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Token condiviso per endpoint interni (Prometheus, webhook-receiver, ecc.).
 * Header: {@code X-Internal-Token}.
 */
@ConfigurationProperties(prefix = "payment.internal")
public class InternalServiceProperties {

    /** Se vuoto, la protezione interna è disabilitata (solo per test unitari). */
    private String token = "";

    private String header = "X-Internal-Token";

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public boolean isEnabled() {
        return token != null && !token.isBlank();
    }
}
