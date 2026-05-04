package com.firstticket.paymentservice.domain;

import com.firstticket.common.persistence.BaseEntity;
import com.firstticket.paymentservice.domain.exception.PaymentErrorCode;
import com.firstticket.paymentservice.domain.exception.PaymentException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "p_payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Payment extends BaseEntity {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "booking_id", nullable = false, unique = true)
    private UUID bookingId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "payment_key", length = 200)
    private String paymentKey;

    @Column(name = "order_id", nullable = false, unique = true, length = 64)
    private String orderId;

    @Column(name = "final_amount", nullable = false)
    private Integer finalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "retry_expired_at")
    private LocalDateTime retryExpiredAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "payment_id")
    private List<PaymentHistory> histories = new ArrayList<>();

    public static Payment create(UUID bookingId, UUID userId, String orderId, Integer finalAmount) {
        return new Payment(
            UUID.randomUUID(),
            bookingId,
            userId,
            null,           // paymentKey (토스 승인 후 채워짐)
            orderId,
            finalAmount,
            PaymentStatus.PENDING,
            LocalDateTime.now(),
            null,           // approvedAt
            null,            // retryExpiredAt
            new ArrayList<>()
        );
    }

    // 결제 승인
    public void confirm(String paymentKey, LocalDateTime approvedAt) {
        if (this.status != PaymentStatus.PENDING && this.status != PaymentStatus.FAILED) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_INVALID_STATUS);
        }
        this.paymentKey = paymentKey;
        this.status = PaymentStatus.SUCCESS;
        this.approvedAt = approvedAt;
        this.histories.add(PaymentHistory.create(this.id, PaymentStatus.SUCCESS, null, null));
    }

    // 결제 실패
    public void fail(int retryTtlSeconds) {
        if (this.status != PaymentStatus.PENDING) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_INVALID_STATUS);
        }
        this.status = PaymentStatus.FAILED;
        this.retryExpiredAt = LocalDateTime.now().plusSeconds(retryTtlSeconds);
        this.histories.add(PaymentHistory.create(this.id, PaymentStatus.FAILED, null, null));
    }

    // 환불
    public void refund() {
        if (this.status != PaymentStatus.SUCCESS) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_INVALID_STATUS);
        }
        this.status = PaymentStatus.REFUNDED;
        this.histories.add(PaymentHistory.create(this.id, PaymentStatus.REFUNDED, null, null));
    }

    // 최종 결제 실패 (선점 시간 만료)
    public void finalFail() {
        if (this.status != PaymentStatus.FAILED) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_INVALID_STATUS);
        }
        this.status = PaymentStatus.FINAL_FAILED;
        this.histories.add(PaymentHistory.create(this.id, PaymentStatus.FINAL_FAILED, null, null));
    }

    // 최종 결제 실패 여부 확인
    public boolean isFinalFailed() {
        LocalDateTime now = LocalDateTime.now();
        return this.status == PaymentStatus.FAILED
            && this.retryExpiredAt != null
            && !now.isBefore(this.retryExpiredAt);
    }
}
