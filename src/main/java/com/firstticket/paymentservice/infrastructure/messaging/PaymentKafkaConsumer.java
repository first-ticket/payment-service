package com.firstticket.paymentservice.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firstticket.paymentservice.application.PaymentCommandService;
import com.firstticket.paymentservice.application.dto.command.RefundPaymentCommand;
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

    @KafkaListener(topics = "booking.payment.refund", groupId = "payment-service")
    public void handleBookingRefund(ConsumerRecord<String, String> record) {
        try {
            BookingRefundPayload payload = objectMapper.readValue(record.value(), BookingRefundPayload.class);

            log.info("booking.payment.refund 수신 - paymentId: {}, reason: {}",
                payload.paymentId(), payload.reason());

            paymentCommandService.refundPayment(
                new RefundPaymentCommand(
                    payload.paymentId(),
                    payload.userId(),
                    payload.reason()
                )
            );
        } catch (Exception e) {
            log.error("booking.payment.refund 처리 실패 - {}", e.getMessage(), e);
            throw new RuntimeException(e); // 예외 재전파 → Spring Kafka 재시도
        }
    }
}
