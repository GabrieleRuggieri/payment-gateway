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
 * API REST per i merchant: avvio pagamenti e consultazione dello stato.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Pagamenti", description = "Avvio pagamenti, idempotenza e consultazione stato")
public class PaymentController {

    private static final Pattern IDEMPOTENCY_KEY_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_-]{8,255}$");

    private final PaymentService paymentService;

    /**
     * Crea un nuovo pagamento. Fornire una {@code Idempotency-Key} univoca per ogni tentativo logico.
     */
    @PostMapping
    @Operation(
            summary = "Crea pagamento",
            description = """
                    Avvia la saga di pagamento. Richiede l'header `Idempotency-Key` (consigliato UUID v4).
                    I retry con la stessa chiave restituiscono la risposta originale con `Idempotent-Replayed: true`.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Pagamento creato o risposta idempotente ripetuta",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class)),
            headers = @Header(
                    name = TopicConstants.HEADER_IDEMPOTENT_REPLAYED,
                    description = "Impostato a true in caso di replay idempotente",
                    schema = @Schema(type = "string")
            )
    )
    public ResponseEntity<PaymentResponse> createPayment(
            @Parameter(description = "Chiave univoca generata dal client (min 8 caratteri)", required = true)
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
     * Restituisce lo stato corrente dell'aggregato — utile per il polling del frontend durante la saga.
     */
    @GetMapping("/{paymentId}")
    @Operation(summary = "Recupera pagamento per id", description = "Polling dello stato durante l'esecuzione della saga")
    public PaymentResponse getPayment(
            @Parameter(description = "UUID del pagamento") @PathVariable UUID paymentId) {
        return paymentService.getPayment(paymentId);
    }

    private void validateIdempotencyKey(String key) {
        if (key == null || !IDEMPOTENCY_KEY_PATTERN.matcher(key).matches()) {
            throw new IllegalArgumentException(
                    "Idempotency-Key must be 8-255 alphanumeric characters (UUID recommended)");
        }
    }
}
