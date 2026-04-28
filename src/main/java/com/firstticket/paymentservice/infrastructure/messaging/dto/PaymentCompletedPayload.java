package com.firstticket.paymentservice.infrastructure.messaging.dto;

import com.firstticket.paymentservice.domain.Payment;

import java.util.UUID;

public record PaymentCompletedPayload(
    UUID paymentId,
    UUID bookingId,
    UUID userId,
    Integer amount,
    String status
) {
    public static PaymentCompletedPayload from(Payment payment) {
        return new PaymentCompletedPayload(
            payment.getId(),
            payment.getBookingId(),
            payment.getUserId(),
            payment.getFinalAmount(),
            payment.getStatus().name()
        );
    }
}
