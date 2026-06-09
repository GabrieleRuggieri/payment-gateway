package com.finance.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Configurazione web MVC, inclusi i mapping CORS per il frontend. */
@Configuration
public class WebConfig {

    @Bean
    WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("http://localhost:3000", "http://localhost:5173", "http://localhost")
                        .allowedMethods("GET", "POST", "PUT", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders("Idempotent-Replayed");
            }
        };
    }
}
