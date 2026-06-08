package com.finance.payment.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    /**
     * TODO: Replace RestTemplate with WebClient + Resilience4j for webhook delivery.
     */
    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
