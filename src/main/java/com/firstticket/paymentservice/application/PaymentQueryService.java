package com.firstticket.paymentservice.application;

import com.firstticket.paymentservice.application.dto.result.PaymentResult;
import com.firstticket.paymentservice.domain.Payment;
import com.firstticket.paymentservice.domain.PaymentRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentQueryService {

    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public PaymentResult getPayment(UUID paymentId, UUID userId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("결제를 찾을 수 없습니다."));

        if (!payment.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 결제만 조회할 수 있습니다.");
        }

        return PaymentResult.from(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResult> getMyPayments(UUID userId) {
        return paymentRepository.findAllByUserId(userId)
            .stream()
            .map(PaymentResult::from)
            .toList();
    }
}
