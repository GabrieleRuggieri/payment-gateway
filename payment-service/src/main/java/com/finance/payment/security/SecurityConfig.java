package com.finance.payment.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/** Configurazione Spring Security: autenticazione API key e rate limiting. */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({PaymentSecurityProperties.class, RateLimitProperties.class})
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ApiKeyAuthenticationFilter apiKeyAuthenticationFilter,
            ApiRateLimitFilter apiRateLimitFilter) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/prometheus",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/api-docs",
                                "/api-docs/**"
                        ).permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(apiRateLimitFilter, ApiKeyAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    ApiKeyAuthenticationFilter apiKeyAuthenticationFilter(
            MerchantApiKeyRepository apiKeyRepository,
            PaymentSecurityProperties securityProperties) {
        return new ApiKeyAuthenticationFilter(apiKeyRepository, securityProperties);
    }

    @Bean
    ApiRateLimitFilter apiRateLimitFilter(
            org.springframework.data.redis.core.StringRedisTemplate redisTemplate,
            RateLimitProperties rateLimitProperties) {
        return new ApiRateLimitFilter(redisTemplate, rateLimitProperties);
    }
}
