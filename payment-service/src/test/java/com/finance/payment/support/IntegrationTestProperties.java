package com.finance.payment.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.ConfluentKafkaContainer;

/**
 * Shared dynamic properties for Testcontainers-based integration tests.
 */
public final class IntegrationTestProperties {

    private IntegrationTestProperties() {
    }

    public static void registerDatastores(
            DynamicPropertyRegistry registry,
            PostgreSQLContainer<?> postgres,
            ConfluentKafkaContainer kafka) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> 6379);
        registry.add("management.health.redis.enabled", () -> "false");
        registry.add("management.health.kafka.enabled", () -> "false");
        registry.add("payment.rate-limit.enabled", () -> "false");
    }
}
