package com.firstticket.paymentservice.application;

import com.firstticket.paymentservice.application.dto.result.PaymentResult;
import com.firstticket.paymentservice.domain.Payment;
import com.firstticket.paymentservice.domain.PaymentRepository;
import com.firstticket.paymentservice.domain.exception.PaymentErrorCode;
import com.firstticket.paymentservice.domain.exception.PaymentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PaymentQueryServiceTest {

    @InjectMocks
    private PaymentQueryService paymentQueryService;

    @Mock
    private PaymentRepository paymentRepository;

    private Payment createSuccessPayment(UUID userId) {
        Payment payment = Payment.create(
            UUID.randomUUID(),
            userId,
            UUID.randomUUID().toString().replace("-", ""),
            50000
        );
        payment.confirm("paymentKey", LocalDateTime.now());
        return payment;
    }

    @Test
    @DisplayName("결제 상세 조회 성공")
    void get_payment_success() {
        // given
        UUID userId = UUID.randomUUID();
        Payment payment = createSuccessPayment(userId);
        given(paymentRepository.findById(payment.getId()))
            .willReturn(Optional.of(payment));

        // when
        PaymentResult result = paymentQueryService.getPayment(payment.getId(), userId);

        // then
        assertThat(result.status()).isEqualTo(payment.getStatus());
    }

    @Test
    @DisplayName("존재하지 않는 결제 조회 시 예외")
    void get_payment_not_found_throws_exception() {
        // given
        given(paymentRepository.findById(any()))
            .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentQueryService.getPayment(UUID.randomUUID(), UUID.randomUUID()))
            .isInstanceOf(PaymentException.class)
            .satisfies(e -> assertThat(((PaymentException) e).getErrorCode())
                .isEqualTo(PaymentErrorCode.PAYMENT_NOT_FOUND));
    }

    @Test
    @DisplayName("본인 결제가 아닌 경우 조회 시 예외")
    void get_payment_forbidden_throws_exception() {
        // given
        Payment payment = createSuccessPayment(UUID.randomUUID());
        given(paymentRepository.findById(payment.getId()))
            .willReturn(Optional.of(payment));

        // when & then
        assertThatThrownBy(() -> paymentQueryService.getPayment(payment.getId(), UUID.randomUUID()))
            .isInstanceOf(PaymentException.class)
            .satisfies(e -> assertThat(((PaymentException) e).getErrorCode())
                .isEqualTo(PaymentErrorCode.PAYMENT_FORBIDDEN));
    }

    @Test
    @DisplayName("내 결제 목록 조회 성공")
    void get_my_payments_success() {
        // given
        UUID userId = UUID.randomUUID();
        Payment payment = createSuccessPayment(userId);
        given(paymentRepository.findAllByUserIdWithHistories(userId))
            .willReturn(List.of(payment));

        // when
        List<PaymentResult> results = paymentQueryService.getMyPayments(userId);

        // then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).status()).isEqualTo(payment.getStatus());
    }

    @Test
    @DisplayName("내 결제 목록 조회 시 빈 목록 반환")
    void get_my_payments_empty() {
        // given
        UUID userId = UUID.randomUUID();
        given(paymentRepository.findAllByUserIdWithHistories(userId))
            .willReturn(List.of());

        // when
        List<PaymentResult> results = paymentQueryService.getMyPayments(userId);

        // then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("전체 결제 목록 조회 성공 (어드민)")
    void get_all_payments_success() {
        // given
        UUID userId = UUID.randomUUID();
        Payment payment = createSuccessPayment(userId);
        Page<Payment> page = new PageImpl<>(List.of(payment));
        given(paymentRepository.findAll(any()))
            .willReturn(page);

        // when
        Page<PaymentResult> results = paymentQueryService.getAllPayments(PageRequest.of(0, 10));

        // then
        assertThat(results.getContent()).hasSize(1);
    }
}
