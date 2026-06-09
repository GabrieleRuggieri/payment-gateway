package com.finance.payment.authorization;

import com.finance.payment.common.saga.SagaEventDedupAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/** Punto di ingresso del servizio di autorizzazione pagamenti. */
@SpringBootApplication
@Import(SagaEventDedupAutoConfiguration.class)
public class AuthorizationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthorizationServiceApplication.class, args);
    }
}
