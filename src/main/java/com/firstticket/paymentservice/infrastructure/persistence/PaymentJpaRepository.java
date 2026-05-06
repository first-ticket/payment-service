package com.firstticket.paymentservice.infrastructure.persistence;

import com.firstticket.paymentservice.domain.Payment;
import com.firstticket.paymentservice.domain.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentJpaRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByOrderId(String orderId);

    Optional<Payment> findByBookingId(UUID bookingId);

    List<Payment> findAllByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Payment> findAllByStatusAndRequestedAtBefore(PaymentStatus status, LocalDateTime expiredAt);
}
