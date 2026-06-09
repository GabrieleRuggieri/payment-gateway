package com.finance.payment.security;

import java.util.UUID;

/**
 * Principal Spring Security del merchant autenticato.
 *
 * @param merchantId identificativo del merchant
 * @param keyLabel   etichetta descrittiva della chiave API
 */
public record MerchantPrincipal(UUID merchantId, String keyLabel) {
}
