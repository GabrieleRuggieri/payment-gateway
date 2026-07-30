package com.finance.payment.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Protegge endpoint sensibili (prometheus, swagger) con {@code X-Internal-Token}
 * oppure {@code Authorization: Bearer &lt;token&gt;} (Prometheus scrape).
 */
@RequiredArgsConstructor
public class InternalTokenFilter extends OncePerRequestFilter {

    private final InternalServiceProperties properties;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return true;
        }
        String path = request.getRequestURI();
        return !(path.startsWith("/actuator/prometheus")
                || path.startsWith("/swagger-ui")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/api-docs"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String provided = request.getHeader(properties.getHeader());
        if (provided == null || provided.isBlank()) {
            String auth = request.getHeader("Authorization");
            if (auth != null && auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
                provided = auth.substring(7).trim();
            }
        }
        if (provided == null || !provided.equals(properties.getToken())) {
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
