package com.finance.payment.security;

import java.util.UUID;

public record MerchantPrincipal(UUID merchantId, String keyLabel) {
}
