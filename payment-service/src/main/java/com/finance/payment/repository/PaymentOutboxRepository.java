package com.finance.payment.repository;

import com.finance.payment.domain.PaymentOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/** Repository JPA per la tabella outbox transazionale. */
public interface PaymentOutboxRepository extends JpaRepository<PaymentOutbox, Long> {

    List<PaymentOutbox> findByAggregateId(UUID aggregateId);

    /**
     * Recupera le righe outbox in sospeso con lock a livello riga, saltando quelle bloccate da altre istanze relay.
     */
    @Query(value = """
            SELECT * FROM payment_outbox
            WHERE status = 'PENDING'
            ORDER BY created_at ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<PaymentOutbox> findPendingWithLock(@Param("limit") int limit);
}
