package com.firstticket.paymentservice.application.dto.result;

import com.firstticket.paymentservice.domain.Payment;
import com.firstticket.paymentservice.domain.PaymentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResult(
    UUID paymentId,
    UUID userId,
    String orderId,
    Integer amount,
    PaymentStatus status,
    LocalDateTime requestedAt,
    LocalDateTime approvedAt
) {
    public static PaymentResult from(Payment payment) {
        return new PaymentResult(
            payment.getId(),
            payment.getUserId(),
            payment.getOrderId(),
            payment.getFinalAmount(),
            payment.getStatus(),
            payment.getRequestedAt(),
            payment.getApprovedAt()
        );
    }
}
