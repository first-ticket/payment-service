package com.firstticket.paymentservice.infrastructure.messaging.dto;

import com.firstticket.paymentservice.domain.Payment;

import java.util.UUID;

public record PaymentRefundCompletedPayload(
    UUID paymentId,
    UUID bookingId,
    UUID userId
) {
    public static PaymentRefundCompletedPayload from(Payment payment) {
        return new PaymentRefundCompletedPayload(
            payment.getId(),
            payment.getBookingId(),
            payment.getUserId()
        );
    }
}
