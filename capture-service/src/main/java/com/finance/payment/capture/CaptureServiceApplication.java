package com.finance.payment.capture;

import com.finance.payment.common.saga.SagaEventDedupAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/** Punto di ingresso del servizio di capture pagamenti. */
@SpringBootApplication
@Import(SagaEventDedupAutoConfiguration.class)
public class CaptureServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CaptureServiceApplication.class, args);
    }
}
