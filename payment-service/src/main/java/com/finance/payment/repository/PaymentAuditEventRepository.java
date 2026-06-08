package com.finance.payment.repository;

import com.finance.payment.domain.PaymentAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentAuditEventRepository extends JpaRepository<PaymentAuditEvent, Long> {
}
