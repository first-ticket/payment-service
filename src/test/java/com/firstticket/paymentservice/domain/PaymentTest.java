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

    @Test
    @DisplayName("선점 시간 만료 전에는 isFinalFailed false")
    void is_not_final_failed_within_retry_time() {
        Payment payment = createPayment();
        payment.fail(300); // 300초 후 만료
        assertThat(payment.isFinalFailed()).isFalse();
    }

    @Test
    @DisplayName("선점 시간 만료 후에는 isFinalFailed true")
    void is_final_failed_after_retry_expired() {
        Payment payment = createPayment();
        payment.fail(-1); // 이미 만료 (-1초)
        assertThat(payment.isFinalFailed()).isTrue();
    }

    @Test
    @DisplayName("최종 실패 시 FINAL_FAILED 상태")
    void final_fail_payment_status_final_failed() {
        Payment payment = createPayment();
        payment.fail(-1); // 이미 만료
        payment.finalFail();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FINAL_FAILED);
    }

    @Test
    @DisplayName("FINAL_FAILED 상태에서 confirm 시 예외")
    void confirm_final_failed_payment_throws_exception() {
        Payment payment = createPayment();
        payment.fail(-1);
        payment.finalFail();
        assertThatThrownBy(() -> payment.confirm("paymentKey", LocalDateTime.now()))
            .isInstanceOf(PaymentException.class)
            .satisfies(e -> assertThat(((PaymentException) e).getErrorCode())
                .isEqualTo(PaymentErrorCode.PAYMENT_INVALID_STATUS));
    }

    @Test
    @DisplayName("선점 시간 만료 전에는 finalFail 호출 시 예외")
    void final_fail_before_expired_throws_exception() {
        Payment payment = createPayment();
        payment.fail(300); // 아직 만료 안 됨
        assertThatThrownBy(() -> payment.finalFail())
            .isInstanceOf(PaymentException.class)
            .satisfies(e -> assertThat(((PaymentException) e).getErrorCode())
                .isEqualTo(PaymentErrorCode.PAYMENT_INVALID_STATUS));
    }

    // 1. FAILED 상태에서 confirm 성공 (재시도)
    @Test
    @DisplayName("FAILED 상태에서 재시도 시 SUCCESS 상태")
    void confirm_failed_payment_status_success() {
        Payment payment = createPayment();
        payment.fail(300);
        payment.confirm("paymentKey", LocalDateTime.now());
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    // 2. PENDING 상태에서 fail 두 번 호출 시 예외
    @Test
    @DisplayName("FAILED 상태에서 fail 호출 시 예외")
    void fail_already_failed_throws_exception() {
        Payment payment = createPayment();
        payment.fail(300);
        assertThatThrownBy(() -> payment.fail(300))
            .isInstanceOf(PaymentException.class)
            .satisfies(e -> assertThat(((PaymentException) e).getErrorCode())
                .isEqualTo(PaymentErrorCode.PAYMENT_INVALID_STATUS));
    }

    // 3. SUCCESS 상태에서 환불 시 REFUNDED
    @Test
    @DisplayName("SUCCESS 상태에서만 환불 가능")
    void refund_only_success_payment() {
        Payment payment = createPayment();
        payment.confirm("paymentKey", LocalDateTime.now());
        payment.refund();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    // 4. REFUNDED 상태에서 환불 시 예외
    @Test
    @DisplayName("이미 환불된 결제 환불 시 예외")
    void refund_already_refunded_throws_exception() {
        Payment payment = createPayment();
        payment.confirm("paymentKey", LocalDateTime.now());
        payment.refund();
        assertThatThrownBy(() -> payment.refund())
            .isInstanceOf(PaymentException.class)
            .satisfies(e -> assertThat(((PaymentException) e).getErrorCode())
                .isEqualTo(PaymentErrorCode.PAYMENT_INVALID_STATUS));
    }

    // 5. FINAL_FAILED 상태에서 환불 시 예외
    @Test
    @DisplayName("FINAL_FAILED 상태에서 환불 시 예외")
    void refund_final_failed_throws_exception() {
        Payment payment = createPayment();
        payment.fail(-1);
        payment.finalFail();
        assertThatThrownBy(() -> payment.refund())
            .isInstanceOf(PaymentException.class)
            .satisfies(e -> assertThat(((PaymentException) e).getErrorCode())
                .isEqualTo(PaymentErrorCode.PAYMENT_INVALID_STATUS));
    }

    // 6. 결제 생성 시 histories 비어있음
    @Test
    @DisplayName("결제 생성 시 histories 비어있음")
    void create_payment_histories_empty() {
        Payment payment = createPayment();
        assertThat(payment.getHistories()).isEmpty();
    }

    // 7. 결제 승인 시 histories에 SUCCESS 추가
    @Test
    @DisplayName("결제 승인 시 histories에 SUCCESS 추가")
    void confirm_payment_add_history() {
        Payment payment = createPayment();
        payment.confirm("paymentKey", LocalDateTime.now());
        assertThat(payment.getHistories()).hasSize(1);
        assertThat(payment.getHistories().get(0).getStatus())
            .isEqualTo(PaymentStatus.SUCCESS);
    }

    // 8. 결제 생성 시 requestedAt 설정
    @Test
    @DisplayName("결제 생성 시 requestedAt 설정됨")
    void create_payment_requested_at_not_null() {
        Payment payment = createPayment();
        assertThat(payment.getRequestedAt()).isNotNull();
    }
}
