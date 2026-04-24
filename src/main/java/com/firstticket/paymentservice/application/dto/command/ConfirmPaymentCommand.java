package com.firstticket.paymentservice.application.dto.command;

public record ConfirmPaymentCommand(
    String paymentKey,
    String orderId,
    Integer amount
) {}
