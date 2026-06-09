package com.finance.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Audit trail immutabile delle transizioni di stato del pagamento.
 */
@Entity
@Table(name = "payment_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "old_status")
    private String oldStatus;

    @Column(name = "new_status")
    private String newStatus;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> payload;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by")
    private String createdBy;

    public static PaymentAuditEvent of(
            UUID paymentId,
            String eventType,
            PaymentStatus oldStatus,
            PaymentStatus newStatus,
            Map<String, Object> payload,
            String createdBy) {

        PaymentAuditEvent event = new PaymentAuditEvent();
        event.paymentId = paymentId;
        event.eventType = eventType;
        event.oldStatus = oldStatus != null ? oldStatus.name() : null;
        event.newStatus = newStatus != null ? newStatus.name() : null;
        event.payload = payload;
        event.createdBy = createdBy;
        return event;
    }
}
