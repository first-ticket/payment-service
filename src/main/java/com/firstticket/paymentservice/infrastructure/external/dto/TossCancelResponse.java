package com.firstticket.paymentservice.infrastructure.external.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.firstticket.paymentservice.domain.service.dto.TossCancelResult;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossCancelResponse(
    String paymentKey,
    String orderId,
    Integer cancelAmount,
    LocalDateTime canceledAt
) {
    public TossCancelResult toResult() {
        return new TossCancelResult(
            this.paymentKey,
            this.orderId,
            this.cancelAmount,
            this.canceledAt
        );
    }
}
