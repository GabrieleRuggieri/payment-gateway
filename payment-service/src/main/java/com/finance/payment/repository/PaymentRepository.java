package com.finance.payment.repository;

import com.finance.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/** Repository JPA per l'aggregato {@link Payment}. */
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    @Query(value = """
            SELECT * FROM payments
            WHERE metadata->>'processorPaymentIntentId' = :piId
            LIMIT 1
            """, nativeQuery = true)
    Optional<Payment> findByProcessorPaymentIntentId(@Param("piId") String piId);
}
