package com.finance.payment.security;

import com.finance.payment.domain.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MerchantAccessGuard {

    private static final UUID DEMO_MERCHANT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    private final PaymentSecurityProperties securityProperties;

    public UUID currentMerchantId() {
        if (!securityProperties.isEnabled()) {
            return DEMO_MERCHANT_ID;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof ApiKeyAuthentication apiKeyAuth) {
            return apiKeyAuth.getPrincipal().merchantId();
        }
        throw new AccessDeniedException("Merchant not authenticated");
    }

    public void assertMerchantMatches(UUID requestedMerchantId) {
        if (!currentMerchantId().equals(requestedMerchantId)) {
            throw new AccessDeniedException("Merchant id does not match authenticated API key");
        }
    }

    public void assertOwns(Payment payment) {
        if (!payment.getMerchantId().equals(currentMerchantId())) {
            throw new AccessDeniedException("Payment does not belong to authenticated merchant");
        }
    }
}
