package com.firstticket.paymentservice.application.dto.command;

import java.util.UUID;

public record CreatePaymentCommand(
    UUID bookingId,
    UUID userId,
    Integer finalAmount
) {}
