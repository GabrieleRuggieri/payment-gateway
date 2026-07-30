package com.finance.payment.api;

import com.finance.payment.stripe.StripeWebhookService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint pubblico (no API key) per webhook Stripe.
 * In locale: {@code stripe listen --forward-to localhost:8080/api/v1/stripe/webhooks}.
 */
@RestController
@RequestMapping("/api/v1/stripe")
@RequiredArgsConstructor
@Slf4j
@Hidden
public class StripeWebhookController {

    private final StripeWebhookService stripeWebhookService;

    @PostMapping("/webhooks")
    public ResponseEntity<String> receive(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        try {
            if (signature == null || signature.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing Stripe-Signature");
            }
            stripeWebhookService.handleRawEvent(payload, signature);
            return ResponseEntity.ok("ok");
        } catch (IllegalStateException e) {
            log.warn("Stripe webhook rejected (config): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Stripe webhook signature/payload invalid: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("invalid signature");
        } catch (Exception e) {
            log.error("Stripe webhook processing failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error");
        }
    }
}
