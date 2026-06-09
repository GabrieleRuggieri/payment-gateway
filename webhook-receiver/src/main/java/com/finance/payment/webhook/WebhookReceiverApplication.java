package com.finance.payment.webhook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Punto di ingresso del receiver webhook di demo per i merchant. */
@SpringBootApplication
public class WebhookReceiverApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebhookReceiverApplication.class, args);
    }
}
