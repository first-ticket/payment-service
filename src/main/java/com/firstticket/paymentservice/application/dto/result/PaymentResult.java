package com.firstticket.paymentservice.application.dto.result;

import com.firstticket.paymentservice.domain.Payment;

import java.util.UUID;

public record PaymentResult(
    UUID paymentId,
    String orderId,
    Integer amount
) {
    public static PaymentResult from(Payment payment) {
        return new PaymentResult(
            payment.getId(),
            payment.getOrderId(),
            payment.getFinalAmount()
        );
    }
}
