package com.finance.payment.api;

import com.finance.payment.api.dto.CreatePaymentRequest;
import com.finance.payment.api.dto.IdempotentResult;
import com.finance.payment.api.dto.PaymentResponse;
import com.finance.payment.common.kafka.TopicConstants;
import com.finance.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
import java.util.regex.Pattern;

/**
 * REST API for merchants to initiate payments and poll status.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment initiation, idempotency and status queries")
public class PaymentController {

    private static final Pattern IDEMPOTENCY_KEY_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_-]{8,255}$");

    private final PaymentService paymentService;

    /**
     * Creates a new payment. Supply a unique {@code Idempotency-Key} per logical payment attempt.
     */
    @PostMapping
    @Operation(
            summary = "Create payment",
            description = """
                    Initiates a payment saga. Requires header `Idempotency-Key` (UUID v4 recommended).
                    Retries with the same key return the original response with `Idempotent-Replayed: true`.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Payment created or replayed",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class)),
            headers = @Header(
                    name = TopicConstants.HEADER_IDEMPOTENT_REPLAYED,
                    description = "Set to true on idempotent replay",
                    schema = @Schema(type = "string")
            )
    )
    public ResponseEntity<PaymentResponse> createPayment(
            @Parameter(description = "Client-generated unique key (min 8 chars)", required = true)
            @RequestHeader(value = TopicConstants.HEADER_IDEMPOTENCY_KEY) String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) {

        validateIdempotencyKey(idempotencyKey);

        IdempotentResult<PaymentResponse> result = paymentService.initiatePayment(request, idempotencyKey);

        HttpHeaders headers = new HttpHeaders();
        if (result.replayed()) {
            headers.add(TopicConstants.HEADER_IDEMPOTENT_REPLAYED, "true");
        }

        return ResponseEntity.ok().headers(headers).body(result.value());
    }

    /**
     * Returns the current aggregate state — useful for frontend polling during the saga.
     */
    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment by id", description = "Poll payment status during saga execution")
    public PaymentResponse getPayment(
            @Parameter(description = "Payment UUID") @PathVariable UUID paymentId) {
        return paymentService.getPayment(paymentId);
    }

    private void validateIdempotencyKey(String key) {
        if (key == null || !IDEMPOTENCY_KEY_PATTERN.matcher(key).matches()) {
            throw new IllegalArgumentException(
                    "Idempotency-Key must be 8-255 alphanumeric characters (UUID recommended)");
        }
    }
}
