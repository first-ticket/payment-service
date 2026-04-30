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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PaymentCommandServiceTest {

    @InjectMocks
    private PaymentCommandService paymentCommandService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private TossPaymentsPort tossPaymentsPort;

    private Payment createSuccessPayment() {
        Payment payment = Payment.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "orderId123",
            50000
        );
        payment.confirm("paymentKey123", LocalDateTime.now());
        return payment;
    }

    @Test
    @DisplayName("결제 생성 성공")
    void create_payment_success() {
        // given
        CreatePaymentCommand command = new CreatePaymentCommand(
            UUID.randomUUID(), UUID.randomUUID(), 50000
        );
        given(paymentRepository.findByBookingId(command.bookingId()))
            .willReturn(Optional.empty());
        given(paymentRepository.save(any())).willAnswer(i -> i.getArgument(0));

        // when
        PaymentResult result = paymentCommandService.createPayment(command);

        // then
        assertThat(result.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(result.amount()).isEqualTo(50000);
    }

    @Test
    @DisplayName("같은 bookingId로 중복 결제 생성 시 기존 결제 반환")
    void create_payment_duplicate_returns_existing() {
        // given
        Payment existingPayment = Payment.create(
            UUID.randomUUID(), UUID.randomUUID(), "orderId", 50000
        );
        CreatePaymentCommand command = new CreatePaymentCommand(
            existingPayment.getBookingId(), existingPayment.getUserId(), 50000
        );
        given(paymentRepository.findByBookingId(command.bookingId()))
            .willReturn(Optional.of(existingPayment));

        // when
        PaymentResult result = paymentCommandService.createPayment(command);

        // then
        assertThat(result.orderId()).isEqualTo(existingPayment.getOrderId());
    }

    @Test
    @DisplayName("결제 승인 성공")
    void confirm_payment_success() {
        // given
        Payment payment = Payment.create(
            UUID.randomUUID(), UUID.randomUUID(), "orderId123", 50000
        );
        ConfirmPaymentCommand command = new ConfirmPaymentCommand(
            "paymentKey123", "orderId123", 50000
        );
        TossConfirmResult tossResult = new TossConfirmResult(
            "paymentKey123", "orderId123", 50000, "DONE", LocalDateTime.now()
        );
        given(paymentRepository.findByOrderId(command.orderId()))
            .willReturn(Optional.of(payment));
        given(tossPaymentsPort.confirm(any(), any(), any()))
            .willReturn(tossResult);
        given(paymentRepository.save(any())).willAnswer(i -> i.getArgument(0));

        // when
        PaymentResult result = paymentCommandService.confirmPayment(command);

        // then
        assertThat(result.status()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    @DisplayName("금액 위변조 시 예외")
    void confirm_payment_amount_mismatch_throws_exception() {
        // given
        Payment payment = Payment.create(
            UUID.randomUUID(), UUID.randomUUID(), "orderId123", 50000
        );
        ConfirmPaymentCommand command = new ConfirmPaymentCommand(
            "paymentKey123", "orderId123", 99999 // 다른 금액
        );
        given(paymentRepository.findByOrderId(command.orderId()))
            .willReturn(Optional.of(payment));

        // when & then
        assertThatThrownBy(() -> paymentCommandService.confirmPayment(command))
            .isInstanceOf(PaymentException.class)
            .satisfies(e -> assertThat(((PaymentException) e).getErrorCode())
                .isEqualTo(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH));
    }

    @Test
    @DisplayName("환불 성공")
    void refund_payment_success() {
        // given
        Payment payment = createSuccessPayment();
        RefundPaymentCommand command = new RefundPaymentCommand(
            payment.getId(), payment.getUserId(), "테스트 환불"
        );
        TossCancelResult cancelResult = new TossCancelResult(
            "paymentKey123",
            "orderId123",
            50000,
            LocalDateTime.now()
        );

        given(paymentRepository.findById(command.paymentId()))
            .willReturn(Optional.of(payment));
        given(tossPaymentsPort.cancel(any(), any()))
            .willReturn(cancelResult);
        given(paymentRepository.save(any())).willAnswer(i -> i.getArgument(0));

        // when
        PaymentResult result = paymentCommandService.refundPayment(command);

        // then
        assertThat(result.status()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    @DisplayName("본인 결제가 아닌 경우 환불 시 예외")
    void refund_not_owner_throws_exception() {
        // given
        Payment payment = createSuccessPayment();
        RefundPaymentCommand command = new RefundPaymentCommand(
            payment.getId(), UUID.randomUUID(), "테스트 환불" // 다른 userId
        );
        given(paymentRepository.findById(command.paymentId()))
            .willReturn(Optional.of(payment));

        // when & then
        assertThatThrownBy(() -> paymentCommandService.refundPayment(command))
            .isInstanceOf(PaymentException.class)
            .satisfies(e -> assertThat(((PaymentException) e).getErrorCode())
                .isEqualTo(PaymentErrorCode.PAYMENT_FORBIDDEN));
    }

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @BeforeEach
    void setUp() throws Exception {
        java.lang.reflect.Field field = Events.class.getDeclaredField("publisher");
        field.setAccessible(true);
        field.set(null, eventPublisher);
    }
}
