package com.firstticket.paymentservice.infrastructure.messaging.dto;

import java.util.UUID;

public record BookingCompensationPayload(
    UUID paymentId,
    UUID bookingId,
    UUID userId,
    String reason
) {
}
