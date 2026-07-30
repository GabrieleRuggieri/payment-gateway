package com.finance.payment.webhook;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Richiede {@code X-Internal-Token} su POST /webhooks/* quando il token è configurato.
 * GET lista resta aperto in locale per ispezione demo (solo health + list).
 */
@Component
public class InternalTokenFilter extends OncePerRequestFilter {

    @Value("${payment.internal.token:}")
    private String token;

    @Value("${payment.internal.header:X-Internal-Token}")
    private String header;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (token == null || token.isBlank()) {
            return true;
        }
        String path = request.getRequestURI();
        String method = request.getMethod();
        // Proteggi solo la ricezione webhook (POST); GET ispezione e health restano aperti in demo.
        return !("POST".equalsIgnoreCase(method) && path.startsWith("/webhooks/"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String provided = request.getHeader(header);
        if (provided == null || !provided.equals(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("""
                    {"error":"Unauthorized","message":"Missing or invalid internal token"}
                    """);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
