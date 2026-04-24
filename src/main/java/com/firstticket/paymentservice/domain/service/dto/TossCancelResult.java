package com.firstticket.paymentservice.domain.service.dto;

import java.time.LocalDateTime;

public record TossCancelResult(
    String paymentKey,
    String orderId,
    Integer cancelAmount,
    LocalDateTime canceledAt
) {}
