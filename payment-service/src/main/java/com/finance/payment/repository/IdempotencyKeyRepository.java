package com.finance.payment.repository;

import com.finance.payment.domain.IdempotencyRecord;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Idempotency key persistence in PostgreSQL (durable, ACID).
 * <p>
 * {@link #findByKeyWithLock} serializes concurrent requests; unique PK on {@code key}
 * plus {@link com.finance.payment.service.IdempotencyService} handles insert races.
 */
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyRecord, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM IdempotencyRecord r WHERE r.key = :key")
    Optional<IdempotencyRecord> findByKeyWithLock(@Param("key") String key);
}
