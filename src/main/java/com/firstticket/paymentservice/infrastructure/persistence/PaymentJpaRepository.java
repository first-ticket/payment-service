package com.firstticket.paymentservice.infrastructure.persistence;

import com.firstticket.paymentservice.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentJpaRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByOrderId(String orderId);

    Optional<Payment> findByBookingId(UUID bookingId);

    List<Payment> findAllByUserId(UUID userId);
}
