package com.finance.payment.webhook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WebhookReceiverApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebhookReceiverApplication.class, args);
    }
}
