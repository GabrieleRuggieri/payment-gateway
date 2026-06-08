package com.finance.payment.authorization.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthorizationResult {

    boolean success;
    String authorizationCode;
    String failureReason;

    public static AuthorizationResult success(String authorizationCode) {
        return AuthorizationResult.builder()
                .success(true)
                .authorizationCode(authorizationCode)
                .build();
    }

    public static AuthorizationResult failure(String reason) {
        return AuthorizationResult.builder()
                .success(false)
                .failureReason(reason)
                .build();
    }
}
