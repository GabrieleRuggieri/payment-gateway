package com.finance.payment;

import com.finance.payment.common.saga.SagaEventDedupAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Punto di ingresso del servizio di orchestrazione pagamenti. */
@SpringBootApplication
@EnableScheduling
@Import(SagaEventDedupAutoConfiguration.class)
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
