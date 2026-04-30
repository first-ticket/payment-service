package com.firstticket.paymentservice.domain;

import com.firstticket.paymentservice.domain.exception.PaymentErrorCode;
import com.firstticket.paymentservice.domain.exception.PaymentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    private Payment createPayment() {
        return Payment.create(
            UUID.randomUUID(),                                    // bookingId
            UUID.randomUUID(),                                    // userId
            UUID.randomUUID().toString().replace("-", ""),        // orderId
            50000                                                 // finalAmount
        );
    }

    @Test
    @DisplayName("결제 생성 시 PENDING 상태")
    void create_payment_status_pending() {
        Payment payment = createPayment();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("결제 승인 시 SUCCESS 상태")
    void confirm_payment_status_success() {
        Payment payment = createPayment();
        payment.confirm("paymentKey", LocalDateTime.now());
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    @DisplayName("이미 SUCCESS인 결제 승인 시 예외")
    void confirm_already_success_throws_exception() {
        Payment payment = createPayment();
        payment.confirm("paymentKey", LocalDateTime.now());

        assertThatThrownBy(() -> payment.confirm("paymentKey", LocalDateTime.now()))
            .isInstanceOf(PaymentException.class)
            .satisfies(e -> assertThat(((PaymentException) e).getErrorCode())
                .isEqualTo(PaymentErrorCode.PAYMENT_INVALID_STATUS));
    }

    @Test
    @DisplayName("결제 환불 시 REFUNDED 상태")
    void refund_payment_status_refunded() {
        Payment payment = createPayment();
        payment.confirm("paymentKey", LocalDateTime.now());
        payment.refund();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    @DisplayName("PENDING 상태에서 환불 시 예외")
    void refund_pending_payment_throws_exception() {
        Payment payment = createPayment();

        assertThatThrownBy(() -> payment.refund())
            .isInstanceOf(PaymentException.class)
            .satisfies(e -> assertThat(((PaymentException) e).getErrorCode())
                .isEqualTo(PaymentErrorCode.PAYMENT_INVALID_STATUS));
    }

    @Test
    @DisplayName("결제 실패 시 FAILED 상태")
    void fail_payment_status_failed() {
        Payment payment = createPayment();
        payment.fail(300);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }
}
