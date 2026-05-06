package com.firstticket.paymentservice.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(UUID id);

    Optional<Payment> findByOrderId(String orderId);

    Optional<Payment> findByBookingId(UUID bookingId);

    List<Payment> findAllByUserId(UUID userId);

    Page<Payment> findAll(Pageable pageable);

    List<Payment> findAllByStatusAndRequestedAtBefore(PaymentStatus status, LocalDateTime expiredAt);
}
