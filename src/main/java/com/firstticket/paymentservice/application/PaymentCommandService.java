package com.firstticket.paymentservice.application;

import com.firstticket.paymentservice.application.dto.command.ConfirmPaymentCommand;
import com.firstticket.paymentservice.application.dto.command.CreatePaymentCommand;
import com.firstticket.paymentservice.application.dto.command.RefundPaymentCommand;
import com.firstticket.paymentservice.application.dto.result.PaymentResult;
import com.firstticket.paymentservice.domain.Payment;
import com.firstticket.paymentservice.domain.PaymentRepository;
import com.firstticket.paymentservice.domain.PaymentStatus;
import com.firstticket.paymentservice.domain.exception.PaymentErrorCode;
import com.firstticket.paymentservice.domain.exception.PaymentException;
import com.firstticket.paymentservice.domain.service.TossPaymentsPort;
import com.firstticket.paymentservice.domain.service.dto.TossCancelResult;
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
            .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        // 2. 이미 승인된 결제면 기존 결과 반환 (멱등성)
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return PaymentResult.from(payment);
        }

        // 3. 금액 위변조 검증
        if (!payment.getFinalAmount().equals(command.amount())) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        // 4. 토스 승인 요청
        TossConfirmResult result = tossPaymentsPort.confirm(
            command.paymentKey(),
            command.orderId(),
            command.amount()
        );

        // 토스 응답 검증
        if (!command.paymentKey().equals(result.paymentKey())
            || !command.orderId().equals(result.orderId())
            || !command.amount().equals(result.totalAmount())
            || !"DONE".equals(result.status())) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_CONFIRM_FAILED);
        }

        // 5. 결제 상태 변경
        payment.confirm(result.paymentKey(), result.approvedAt());
        paymentRepository.save(payment);

        return PaymentResult.from(payment);
    }

    @Transactional
    public PaymentResult refundPayment(RefundPaymentCommand command) {
        // 1. 결제 조회
        Payment payment = paymentRepository.findById(command.paymentId())
            .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        // 2. 본인 확인
        if (!payment.getUserId().equals(command.userId())) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_FORBIDDEN);
        }

        // 3. 환불 가능 상태 확인은 Payment.refund()에서 가드로 처리됨

        // 4. 토스 취소 요청
        TossCancelResult cancelResult = tossPaymentsPort.cancel(
            payment.getPaymentKey(),
            command.cancelReason()
        );

        if (cancelResult == null) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_CANCEL_FAILED);
        }

        // 5. 결제 상태 변경
        payment.refund();
        paymentRepository.save(payment);

        return PaymentResult.from(payment);
    }
}
