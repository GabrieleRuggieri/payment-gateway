package com.finance.payment.api.dto;

import java.util.UUID;

/** Configurazione webhook merchant esposta via admin API. */
public record MerchantWebhookResponse(
        UUID merchantId,
        String webhookUrl,
        boolean active,
        /** Secret mascherato (ultimi 4 caratteri). */
        String webhookSecretHint
) {
}
