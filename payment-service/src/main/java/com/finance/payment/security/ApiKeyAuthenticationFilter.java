package com.finance.payment.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final MerchantApiKeyRepository apiKeyRepository;
    private final PaymentSecurityProperties securityProperties;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (!securityProperties.isEnabled()) {
            SecurityContextHolder.getContext().setAuthentication(
                    new ApiKeyAuthentication(new MerchantPrincipal(
                            java.util.UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                            "security-disabled"
                    ))
            );
            filterChain.doFilter(request, response);
            return;
        }

        String rawKey = request.getHeader(securityProperties.getApiKeyHeader());
        if (rawKey == null || rawKey.isBlank()) {
            unauthorized(response, "Missing " + securityProperties.getApiKeyHeader() + " header");
            return;
        }

        String hash = ApiKeyHasher.sha256(rawKey.trim());
        var record = apiKeyRepository.findByApiKeyHashAndActiveTrue(hash);
        if (record.isEmpty()) {
            unauthorized(response, "Invalid API key");
            return;
        }

        MerchantApiKey key = record.get();
        SecurityContextHolder.getContext().setAuthentication(
                new ApiKeyAuthentication(new MerchantPrincipal(key.getMerchantId(), key.getLabel()))
        );
        filterChain.doFilter(request, response);
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("""
                {"error":"Unauthorized","message":"%s"}
                """.formatted(message.replace("\"", "\\\"")));
    }
}
