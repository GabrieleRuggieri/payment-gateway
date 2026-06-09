package com.finance.payment.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Utility per calcolare l'hash SHA-256 delle chiavi API merchant in formato esadecimale. */
final class ApiKeyHasher {

    private ApiKeyHasher() {
    }

    /** Restituisce l'hash SHA-256 della chiave in chiaro, in esadecimale minuscolo. */
    static String sha256(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
