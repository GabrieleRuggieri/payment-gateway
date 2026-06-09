package com.finance.payment.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MerchantApiKeyRepository extends JpaRepository<MerchantApiKey, String> {

    Optional<MerchantApiKey> findByApiKeyHashAndActiveTrue(String apiKeyHash);
}
