package com.firstticket.paymentservice.presentation.dto.response;

import com.firstticket.paymentservice.application.dto.result.PaymentResult;
import com.firstticket.paymentservice.domain.PaymentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
    UUID paymentId,
    UUID userId,
    String orderId,
    Integer amount,
    PaymentStatus status,
    LocalDateTime requestedAt,
    LocalDateTime approvedAt
) {
    public static PaymentResponse from(PaymentResult result) {
        return new PaymentResponse(
            result.paymentId(),
            result.userId(),
            result.orderId(),
            result.amount(),
            result.status(),
            result.requestedAt(),
            result.approvedAt()
        );
    }
}
