package com.firstticket.paymentservice.infrastructure.external.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.firstticket.paymentservice.domain.service.dto.TossConfirmResult;

import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossConfirmResponse(
    String paymentKey,
    String orderId,
    Integer totalAmount,
    String status,
    OffsetDateTime approvedAt
) {
    public TossConfirmResult toResult() {
        return new TossConfirmResult(
            this.paymentKey,
            this.orderId,
            this.totalAmount,
            this.status,
            this.approvedAt != null ? this.approvedAt.toLocalDateTime() : null
        );
    }
}
