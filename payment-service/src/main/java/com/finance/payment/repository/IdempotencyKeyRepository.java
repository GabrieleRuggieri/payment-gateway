package com.finance.payment.repository;

import com.finance.payment.domain.IdempotencyRecord;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

/**
 * Persistenza delle chiavi di idempotenza su PostgreSQL (durabile, ACID).
 * <p>
 * {@link #findByKeyWithLock} serializza le richieste concorrenti; la PK univoca su {@code key}
 * insieme a {@link com.finance.payment.service.IdempotencyService} gestisce le race in inserimento.
 */
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyRecord, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM IdempotencyRecord r WHERE r.key = :key")
    Optional<IdempotencyRecord> findByKeyWithLock(@Param("key") String key);

    @Modifying
    @Query("DELETE FROM IdempotencyRecord r WHERE r.expiresAt < :cutoff")
    int deleteByExpiresAtBefore(@Param("cutoff") Instant cutoff);
}
