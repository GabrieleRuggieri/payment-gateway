package com.finance.payment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configurazione OpenAPI/Swagger per la documentazione delle API REST. */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI paymentOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Payment Gateway API")
                        .description("Orchestrazione pagamenti con saga, outbox transazionale e idempotenza")
                        .version("0.1.0"));
    }
}
