package com.finance.payment.notification.webhook;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/** Firma HMAC-SHA256 dei webhook merchant ({@code t=<ts>,v1=<hex>}). */
public final class WebhookSigner {

    private WebhookSigner() {
    }

    public static String sign(String secret, long timestampEpochSeconds, String body) {
        String payload = timestampEpochSeconds + "." + body;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return "t=" + timestampEpochSeconds + ",v1=" + HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign webhook", e);
        }
    }
}
