package com.firstticket.paymentservice.presentation.dto.response;

import com.firstticket.paymentservice.application.dto.result.PaymentResult;

import java.util.UUID;

public record PaymentResponse(
    UUID paymentId,
    String orderId,
    Integer amount
) {
    public static PaymentResponse from(PaymentResult result) {
        return new PaymentResponse(
            result.paymentId(),
            result.orderId(),
            result.amount()
        );
    }
}
