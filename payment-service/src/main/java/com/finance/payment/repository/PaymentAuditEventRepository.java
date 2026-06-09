package com.finance.payment.repository;

import com.finance.payment.domain.PaymentAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository JPA per gli eventi di audit del pagamento. */
public interface PaymentAuditEventRepository extends JpaRepository<PaymentAuditEvent, Long> {
}
