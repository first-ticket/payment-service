package com.firstticket.paymentservice.presentation.dto.request;

import com.firstticket.paymentservice.application.dto.command.CreatePaymentCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record PaymentCreateRequest(
    @NotNull UUID bookingId,
    @NotNull UUID userId,
    @NotNull @Positive Integer finalAmount
) {
    public CreatePaymentCommand toCommand() {
        return new CreatePaymentCommand(
            this.bookingId,
            this.userId,
            this.finalAmount
        );
    }
}
