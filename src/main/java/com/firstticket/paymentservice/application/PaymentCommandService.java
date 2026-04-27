package com.firstticket.paymentservice.application;

import com.firstticket.paymentservice.application.dto.command.ConfirmPaymentCommand;
import com.firstticket.paymentservice.application.dto.command.CreatePaymentCommand;
import com.firstticket.paymentservice.application.dto.result.PaymentResult;
import com.firstticket.paymentservice.domain.Payment;
import com.firstticket.paymentservice.domain.PaymentRepository;
import com.firstticket.paymentservice.domain.PaymentStatus;
import com.firstticket.paymentservice.domain.service.TossPaymentsPort;
import com.firstticket.paymentservice.domain.service.dto.TossConfirmResult;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentCommandService {

    private final PaymentRepository paymentRepository;
    private final TossPaymentsPort tossPaymentsPort;

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

    @Transactional
    public PaymentResult confirmPayment(ConfirmPaymentCommand command) {
        // 1. orderId로 결제 조회
        Payment payment = paymentRepository.findByOrderId(command.orderId())
            .orElseThrow(() -> new IllegalArgumentException("결제를 찾을 수 없습니다."));

        // 2. 이미 승인된 결제면 기존 결과 반환 (멱등성)
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return PaymentResult.from(payment);
        }

        // 3. 금액 위변조 검증
        if (!payment.getFinalAmount().equals(command.amount())) {
            throw new IllegalArgumentException("결제 금액이 일치하지 않습니다.");
        }

        // 4. 토스 승인 요청
        TossConfirmResult result = tossPaymentsPort.confirm(
            command.paymentKey(),
            command.orderId(),
            command.amount()
        );

        // 5. 결제 상태 변경
        payment.confirm(result.paymentKey(), result.approvedAt());
        paymentRepository.save(payment);

        return PaymentResult.from(payment);
    }
}
