package com.finance.payment.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Upsert configurazione webhook merchant. */
public record UpsertMerchantWebhookRequest(
        @NotBlank @Size(max = 2048) String webhookUrl,
        @NotBlank @Size(min = 8, max = 128) String webhookSecret,
        Boolean active
) {
}
