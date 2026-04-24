package com.firstticket.paymentservice.presentation.dto.request;


import com.firstticket.paymentservice.application.dto.command.ConfirmPaymentCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentConfirmRequest(
    @NotBlank String paymentKey,
    @NotBlank String orderId,
    @NotNull @Positive Integer amount
) {
    public ConfirmPaymentCommand toCommand() {
        return new ConfirmPaymentCommand(
            this.paymentKey,
            this.orderId,
            this.amount
        );
    }
}
