package com.firstticket.paymentservice.domain.service.dto;

import java.time.LocalDateTime;

public record TossConfirmResult(
    String paymentKey,
    String orderId,
    Integer totalAmount,
    String status,
    LocalDateTime approvedAt
) {}
