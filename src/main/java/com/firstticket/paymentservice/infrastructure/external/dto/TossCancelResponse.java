package com.firstticket.paymentservice.infrastructure.external.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.firstticket.paymentservice.domain.service.dto.TossCancelResult;

import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossCancelResponse(
    String paymentKey,
    String orderId,
    Integer cancelAmount,
    OffsetDateTime canceledAt
) {
    public TossCancelResult toResult() {
        return new TossCancelResult(
            this.paymentKey,
            this.orderId,
            this.cancelAmount,
            this.canceledAt != null ? this.canceledAt.toLocalDateTime() : null
        );
    }
}
