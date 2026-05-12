package com.firstticket.paymentservice.infrastructure.persistence;

import com.firstticket.paymentservice.domain.Payment;
import com.firstticket.paymentservice.domain.PaymentRepository;
import com.firstticket.paymentservice.domain.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

    private final PaymentJpaRepository paymentJpaRepository;

    @Override
    public Payment save(Payment payment) {
        return paymentJpaRepository.save(payment);
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return paymentJpaRepository.findById(id);
    }

    @Override
    public Optional<Payment> findByOrderId(String orderId) {
        return paymentJpaRepository.findByOrderId(orderId);
    }

    @Override
    public Optional<Payment> findByBookingId(UUID bookingId) {return paymentJpaRepository.findByBookingId(bookingId);}

    @Override
    public List<Payment> findAllByUserId(UUID userId) {return paymentJpaRepository.findAllByUserId(userId);}

    @Override
    public Page<Payment> findAll(Pageable pageable) {return paymentJpaRepository.findAll(pageable);}

    @Override
    public List<Payment> findAllByStatusAndRequestedAtBefore(PaymentStatus status, LocalDateTime expiredAt) {return paymentJpaRepository.findAllByStatusAndRequestedAtBefore(status, expiredAt);}

    @Override
    public List<Payment> findAllByUserIdWithHistories(UUID userId) {return paymentJpaRepository.findAllByUserIdWithHistories(userId);}
}
