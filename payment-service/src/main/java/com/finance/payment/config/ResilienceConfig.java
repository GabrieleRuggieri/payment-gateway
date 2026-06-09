package com.finance.payment.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.RetryConfig;
import com.finance.payment.common.exception.TemporaryProcessorException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Policy Resilience4j per le chiamate al processore esterno (usate quando il payment-service integra servizi esterni).
 */
@Configuration
public class ResilienceConfig {

    @Bean
    CircuitBreakerConfig processorCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(10)
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();
    }

    @Bean
    RetryConfig processorRetryConfig() {
        return RetryConfig.custom()
                .maxAttempts(3)
                .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(500, 2, 0.3))
                .retryOnException(e -> e instanceof TemporaryProcessorException)
                .build();
    }
}
