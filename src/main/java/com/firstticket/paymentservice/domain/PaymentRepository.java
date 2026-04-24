package com.firstticket.paymentservice.domain;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(UUID id);

    Optional<Payment> findByOrderId(String orderId);

    Optional<Payment> findByBookingId(UUID bookingId);
}
