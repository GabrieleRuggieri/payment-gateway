package com.finance.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Configurazione Redis per funzionalità non critiche (rate limiting, cache).
 * <p>
 * Idempotenza e dedup saga usano PostgreSQL — perdere una chiave Redis potrebbe causare un addebito doppio.
 */
@Configuration
public class RedisConfig {

    @Bean
    StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
