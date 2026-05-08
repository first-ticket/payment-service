package com.firstticket.paymentservice.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firstticket.paymentservice.application.PaymentCommandService;
import com.firstticket.paymentservice.application.dto.command.RefundPaymentCommand;
import com.firstticket.paymentservice.infrastructure.messaging.dto.BookingCompensationPayload;
import com.firstticket.paymentservice.infrastructure.messaging.dto.BookingRefundPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentKafkaConsumer {

    private final PaymentCommandService paymentCommandService;
    private final ObjectMapper objectMapper;

    // 좌석 선점 시간 만료
    @KafkaListener(topics = "${kafka.topics.booking-refund}", groupId = "payment-service")
    public void handleBookingRefund(ConsumerRecord<String, String> record) {
        try {
            BookingRefundPayload payload = objectMapper.readValue(record.value(), BookingRefundPayload.class);

            log.info("booking.refund.request 수신 - paymentId: {}, reason: {}",
                payload.paymentId(), payload.reason());

            paymentCommandService.refundPayment(
                new RefundPaymentCommand(
                    payload.paymentId(),
                    payload.userId(),
                    payload.reason()
                )
            );
        } catch (RuntimeException e) {
            log.error("처리 실패 - {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("처리 실패 - {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    // 사용자 예매 취소
    @KafkaListener(topics = "${kafka.topics.booking-cancel}", groupId = "payment-service")
    public void handleBookingCancel(ConsumerRecord<String, String> record) {
        try {
            BookingRefundPayload payload = objectMapper.readValue(record.value(), BookingRefundPayload.class);

            log.info("booking.cancel.request 수신 - paymentId: {}, reason: {}",
                payload.paymentId(), payload.reason());

            paymentCommandService.refundPayment(
                new RefundPaymentCommand(
                    payload.paymentId(),
                    payload.userId(),
                    payload.reason()
                )
            );
        } catch (RuntimeException e) {
            log.error("처리 실패 - {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("처리 실패 - {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    // 보상 트랜잭션
    @KafkaListener(topics = "${kafka.topics.booking-compensation}", groupId = "payment-service")
    public void handleBookingCompensation(ConsumerRecord<String, String> record) {
        try {
            BookingCompensationPayload payload = objectMapper.readValue(record.value(), BookingCompensationPayload.class);

            log.info("booking.payment.compensation 수신 - paymentId: {}, reason: {}",
                payload.paymentId(), payload.reason());

            paymentCommandService.refundPayment(
                new RefundPaymentCommand(
                    payload.paymentId(),
                    payload.userId(),
                    payload.reason()
                )
            );
        } catch (RuntimeException e) {
            log.error("처리 실패 - {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("처리 실패 - {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
