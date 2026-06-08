package com.finance.payment.repository;

import com.finance.payment.domain.PaymentOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PaymentOutboxRepository extends JpaRepository<PaymentOutbox, Long> {

    List<PaymentOutbox> findByAggregateId(UUID aggregateId);

    /**
     * TODO: Implement native query with SELECT ... FOR UPDATE SKIP LOCKED.
     * JPQL LIMIT/FOR UPDATE support varies — prefer native SQL for production relay.
     *
     * Example:
     * SELECT * FROM payment_outbox
     * WHERE status = 'PENDING'
     * ORDER BY created_at
     * LIMIT :limit
     * FOR UPDATE SKIP LOCKED
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
