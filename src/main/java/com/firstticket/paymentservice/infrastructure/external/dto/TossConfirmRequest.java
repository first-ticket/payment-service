package com.firstticket.paymentservice.infrastructure.external.dto;

public record TossConfirmRequest(
    String paymentKey,
    String orderId,
    Integer amount
) {}
