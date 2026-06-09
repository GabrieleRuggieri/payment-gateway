package com.finance.payment.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

/** Filtro che applica il rate limiting per merchant tramite Redis. */
@RequiredArgsConstructor
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final RateLimitProperties rateLimitProperties;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (!rateLimitProperties.isEnabled() || !request.getRequestURI().startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String bucketKey = resolveBucketKey();
        Long count = redisTemplate.opsForValue().increment(bucketKey);
        if (count != null && count == 1L) {
            redisTemplate.expire(bucketKey, Duration.ofMinutes(1));
        }

        if (count != null && count > rateLimitProperties.getRequestsPerMinute()) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("""
                    {"error":"Too Many Requests","message":"Rate limit exceeded"}
                    """);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveBucketKey() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof ApiKeyAuthentication apiKeyAuth) {
            UUID merchantId = apiKeyAuth.getPrincipal().merchantId();
            return "ratelimit:merchant:" + merchantId;
        }
        return "ratelimit:ip:anonymous";
    }
}
