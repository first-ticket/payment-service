package com.firstticket.paymentservice.domain;

import com.firstticket.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_payments_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentHistory extends BaseEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Column(name = "reason")
    private String reason;

    @Column(name = "toss_raw_response", columnDefinition = "TEXT")
    private String tossRawResponse;

    public static PaymentHistory create(UUID paymentId, PaymentStatus status, String reason, String tossRawResponse) {
        return new PaymentHistory(
            UUID.randomUUID(),
            paymentId,
            status,
            reason,
            tossRawResponse
        );
    }
}
