package com.finance.payment.api;

import com.finance.payment.admin.MerchantWebhookAdminService;
import com.finance.payment.api.dto.MerchantWebhookResponse;
import com.finance.payment.api.dto.UpsertMerchantWebhookRequest;
import com.finance.payment.security.MerchantAccessGuard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Mini admin API: configurazione webhook merchant (protetta da API key + ownership).
 */
@RestController
@RequestMapping("/api/v1/merchants/{merchantId}/webhook")
@RequiredArgsConstructor
@Tag(name = "Merchant webhooks", description = "Configurazione endpoint webhook merchant")
public class MerchantWebhookAdminController {

    private final MerchantWebhookAdminService adminService;
    private final MerchantAccessGuard merchantAccessGuard;

    @GetMapping
    @Operation(summary = "Leggi configurazione webhook merchant")
    public MerchantWebhookResponse get(@PathVariable UUID merchantId) {
        merchantAccessGuard.assertMerchantMatches(merchantId);
        return adminService.get(merchantId);
    }

    @PutMapping
    @Operation(summary = "Crea o aggiorna webhook merchant")
    public MerchantWebhookResponse upsert(
            @PathVariable UUID merchantId,
            @Valid @RequestBody UpsertMerchantWebhookRequest request) {
        merchantAccessGuard.assertMerchantMatches(merchantId);
        return adminService.upsert(merchantId, request);
    }
}
