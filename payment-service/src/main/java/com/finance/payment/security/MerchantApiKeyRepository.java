package com.finance.payment.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Repository JPA per le chiavi API dei merchant. */
public interface MerchantApiKeyRepository extends JpaRepository<MerchantApiKey, String> {

    Optional<MerchantApiKey> findByApiKeyHashAndActiveTrue(String apiKeyHash);
}
