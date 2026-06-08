package com.finance.payment.repository;

import com.finance.payment.domain.IdempotencyRecord;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyRecord, String> {

    /**
     * TODO: Consider INSERT ... ON CONFLICT DO NOTHING for race-free first-write wins.
     * Pessimistic lock helps but does not cover concurrent inserts on missing keys.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM IdempotencyRecord r WHERE r.key = :key")
    Optional<IdempotencyRecord> findByKeyWithLock(@Param("key") String key);
}
