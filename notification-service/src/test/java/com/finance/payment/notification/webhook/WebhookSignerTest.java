package com.finance.payment.notification.webhook;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifica il formato della firma HMAC dei webhook. */
class WebhookSignerTest {

    @Test
    void shouldProduceDeterministicSignature() {
        String a = WebhookSigner.sign("whsec_demo", 1_700_000_000L, "{\"ok\":true}");
        String b = WebhookSigner.sign("whsec_demo", 1_700_000_000L, "{\"ok\":true}");
        assertThat(a).isEqualTo(b);
        assertThat(a).startsWith("t=1700000000,v1=");
        assertThat(a.length()).isGreaterThan(20);
    }
}
