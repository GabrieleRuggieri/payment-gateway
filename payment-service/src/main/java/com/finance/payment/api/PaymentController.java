package com.finance.payment.api;

import com.finance.payment.api.dto.CreatePaymentRequest;
import com.finance.payment.api.dto.PaymentResponse;
import com.finance.payment.common.kafka.TopicConstants;
import com.finance.payment.service.IdempotencyService;
import com.finance.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment initiation and status")
public class PaymentController {

    private final PaymentService paymentService;
    private final IdempotencyService idempotencyService;

    @PostMapping
    @Operation(summary = "Create payment", description = "Requires Idempotency-Key header (UUID v4 recommended)")
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestHeader(value = TopicConstants.HEADER_IDEMPOTENCY_KEY) String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) {

        // TODO: Validate idempotency key format (min 32 chars / UUID)
        PaymentResponse response = paymentService.initiatePayment(request, idempotencyKey);

        HttpHeaders headers = new HttpHeaders();
        if (idempotencyService.wasReplay(idempotencyKey)) {
            headers.add(TopicConstants.HEADER_IDEMPOTENT_REPLAYED, "true");
        }

        return ResponseEntity.ok().headers(headers).body(response);
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment by id")
    public PaymentResponse getPayment(@PathVariable UUID paymentId) {
        return paymentService.getPayment(paymentId);
    }
}
