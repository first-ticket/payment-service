package com.firstticket.paymentservice.application;

import com.firstticket.common.messaging.event.Events;
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
import com.firstticket.paymentservice.infrastructure.messaging.dto.PaymentCompletedPayload;
import com.firstticket.paymentservice.infrastructure.messaging.dto.PaymentFailedPayload;
import com.firstticket.paymentservice.infrastructure.messaging.dto.PaymentRefundCompletedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
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

        // 최종 실패된 결제는 재시도 불가
        if (payment.getStatus() == PaymentStatus.FINAL_FAILED) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_CONFIRM_FAILED);
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

            if (payment.getStatus() == PaymentStatus.PENDING) {
                // 첫 번째 실패 → FAILED 상태로 변경 (재시도 가능)
                payment.fail(300);
                paymentRepository.save(payment);
            } else if (payment.getStatus() == PaymentStatus.FAILED && payment.isFinalFailed()) {
                // 선점 시간 만료 후 재시도 실패 → 최종 실패 상태로 변경 + 이벤트 발행
                payment.finalFail();
                paymentRepository.save(payment);
                Events.publish(
                    UUID.randomUUID().toString(),
                    "PAYMENT",
                    payment.getId(),
                    "payment.failed",
                    PaymentFailedPayload.from(payment, "토스 결제 승인 실패")
                );
            }

            return PaymentResult.from(payment);
        }

        // 5. 결제 상태 변경
        payment.confirm(result.paymentKey(), result.approvedAt());
        paymentRepository.save(payment);

        // 6. 아웃박스 이벤트 저장
        Events.publish(
            UUID.randomUUID().toString(),
            "PAYMENT",
            payment.getId(),
            "payment.completed",
            PaymentCompletedPayload.from(payment)
        );

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

        // 3. 이미 환불된 결제면 그냥 반환 (멱등성)
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            log.info("이미 환불된 결제 - paymentId: {}", command.paymentId());
            return PaymentResult.from(payment);
        }

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

        // 6. 환불 완료 이벤트 발행
        Events.publish(
            UUID.randomUUID().toString(),
            "PAYMENT",
            payment.getId(),
            "payment.refund.completed",
            PaymentRefundCompletedPayload.from(payment)
        );

        return PaymentResult.from(payment);
    }
}
