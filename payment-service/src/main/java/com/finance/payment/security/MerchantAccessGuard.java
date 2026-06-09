package com.finance.payment.security;

import com.finance.payment.domain.Payment;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MerchantAccessGuard {

    public UUID currentMerchantId() {
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
