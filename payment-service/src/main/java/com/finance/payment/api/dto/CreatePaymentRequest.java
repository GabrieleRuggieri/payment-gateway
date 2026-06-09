package com.finance.payment.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Payload di richiesta per l'avvio di un pagamento.
 *
 * @param merchantId   identificativo del merchant
 * @param amount       importo positivo con fino a 4 decimali
 * @param currency     codice ISO 4217 a 3 lettere maiuscole
 * @param description  descrizione opzionale del pagamento
 * @param metadata     metadati aggiuntivi opzionali
 */
public record CreatePaymentRequest(
        @NotNull UUID merchantId,
        @NotNull @DecimalMin(value = "0.0001", message = "Amount must be positive") BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) @Pattern(regexp = "[A-Z]{3}") String currency,
        @Size(max = 500) String description,
        Map<String, Object> metadata
) {
}
