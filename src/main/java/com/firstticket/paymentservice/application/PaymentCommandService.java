package com.firstticket.paymentservice.application;

import com.firstticket.paymentservice.application.dto.command.CreatePaymentCommand;
import com.firstticket.paymentservice.application.dto.result.PaymentResult;
import com.firstticket.paymentservice.domain.Payment;
import com.firstticket.paymentservice.domain.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentCommandService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public PaymentResult createPayment(CreatePaymentCommand command) {
        return paymentRepository.findByBookingId(command.bookingId())
            .map(PaymentResult::from)
            .orElseGet(() -> {
                String orderId = UUID.randomUUID().toString().replace("-", "");

                Payment payment = Payment.create(
                    command.bookingId(),
                    command.userId(),
                    orderId,
                    command.finalAmount()
                );

                try {
                    Payment savedPayment = paymentRepository.save(payment);
                    return PaymentResult.from(savedPayment);
                } catch (DataIntegrityViolationException e) {
                    // 동시 요청으로 unique 제약 위반 시 기존 결제 반환
                    return paymentRepository.findByBookingId(command.bookingId())
                        .map(PaymentResult::from)
                        .orElseThrow(() -> e);
                }
            });
    }
}
