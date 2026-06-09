package com.finance.payment.api.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Risposta di errore standard per le API REST.
 *
 * @param timestamp  istante dell'errore
 * @param status     codice HTTP
 * @param error      categoria sintetica (es. "Not Found")
 * @param message    messaggio descrittivo
 * @param path       URI della richiesta
 * @param details    dettagli per campo (validazione), opzionale
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> details
) {
}
