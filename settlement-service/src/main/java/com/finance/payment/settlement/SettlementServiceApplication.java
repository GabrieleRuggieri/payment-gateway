package com.finance.payment.settlement;

import com.finance.payment.common.saga.SagaEventDedupAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/** Punto di ingresso del servizio di settlement pagamenti. */
@SpringBootApplication
@Import(SagaEventDedupAutoConfiguration.class)
public class SettlementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SettlementServiceApplication.class, args);
    }
}
