package com.firstticket.paymentservice.infrastructure.messaging.dto;

import com.firstticket.paymentservice.domain.Payment;

import java.util.UUID;

public record PaymentFailedPayload(
    UUID paymentId,
    UUID bookingId,
    UUID userId,
    String reason
) {
    public static PaymentFailedPayload from(Payment payment, String reason) {
        return new PaymentFailedPayload(
            payment.getId(),
            payment.getBookingId(),
            payment.getUserId(),
            reason
        );
    }
}
