package com.firstticket.paymentservice.application;

import com.firstticket.common.messaging.event.Events;
import com.firstticket.paymentservice.domain.Payment;
import com.firstticket.paymentservice.domain.PaymentRepository;
import com.firstticket.paymentservice.domain.PaymentStatus;
import com.firstticket.paymentservice.infrastructure.messaging.dto.PaymentFailedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentScheduler {

    private final PaymentRepository paymentRepository;

    @Value("${kafka.topics.payment-failed}")
    private String paymentFailedTopic;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void expirePayments() {
        LocalDateTime expiredAt = LocalDateTime.now().minusSeconds(300); // 추후 설정값으로 변경 예정

        List<Payment> expiredPayments = paymentRepository
            .findAllByStatusAndRequestedAtBefore(PaymentStatus.PENDING, expiredAt);

        for (Payment payment : expiredPayments) {
            try {
                log.info("결제 만료 처리 - paymentId: {}", payment.getId());
                payment.fail(0);
                payment.finalFail();
                paymentRepository.save(payment);

                Events.publish(
                    UUID.randomUUID().toString(),
                    "PAYMENT",
                    payment.getId(),
                    paymentFailedTopic,
                    PaymentFailedPayload.from(payment, "결제 시간 만료")
                );
            } catch (Exception e) {
                log.error("결제 만료 처리 실패 - paymentId: {}", payment.getId(), e);
            }
        }

        if (!expiredPayments.isEmpty()) {
            log.info("만료된 결제 {}건 처리 완료", expiredPayments.size());
        }
    }
}
