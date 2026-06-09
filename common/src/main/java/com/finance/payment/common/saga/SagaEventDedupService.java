package com.finance.payment.common.saga;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Deduplicazione saga su PostgreSQL: INSERT ON CONFLICT DO NOTHING.
 * Scelto al posto di Redis per durabilità e tracciabilità nei flussi di pagamento.
 *
 * <p><strong>Propagation.MANDATORY</strong> è intenzionale: i chiamanti DEVONO essere eseguiti
 * in una transazione attiva affinché l'INSERT di dedup e la logica di business downstream
 * vengano committati atomicamente. Se la transazione del chiamante fa rollback (es. eccezione
 * durante l'elaborazione), anche la riga di dedup viene annullata, consentendo a Kafka di
 * ritentare l'evento correttamente. Senza questo vincolo, un commit separato della riga di dedup
 * impedirebbe ai retry di rielaborare l'evento, instradandolo silenziosamente al DLT.
 */
@RequiredArgsConstructor
public class SagaEventDedupService {

    private static final String INSERT_SQL = """
            INSERT INTO saga_processed_events (consumer_group, payment_id, event_type)
            VALUES (?, ?, ?)
            ON CONFLICT (consumer_group, payment_id, event_type) DO NOTHING
            """;

    private final JdbcTemplate jdbcTemplate;

    /**
     * Registra l'evento come elaborato nella transazione attiva del chiamante.
     *
     * @return {@code true} se l'evento è nuovo e va elaborato; {@code false} se è un duplicato
     * @throws org.springframework.transaction.IllegalTransactionStateException se non esiste una transazione attiva
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean registerIfNew(String consumerGroup, UUID paymentId, String eventType) {
        int inserted = jdbcTemplate.update(INSERT_SQL, consumerGroup, paymentId, eventType);
        return inserted > 0;
    }
}
