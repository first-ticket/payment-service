package com.firstticket.paymentservice.presentation.dto.request;

import com.firstticket.paymentservice.application.dto.command.RefundPaymentCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PaymentRefundRequest(
    @NotBlank String cancelReason
) {
    public RefundPaymentCommand toCommand(UUID paymentId, UUID userId) {
        return new RefundPaymentCommand(
            paymentId,
            userId,
            this.cancelReason
        );
    }
}
