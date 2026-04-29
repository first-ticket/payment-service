package com.firstticket.paymentservice.application;

import com.firstticket.paymentservice.application.dto.result.PaymentResult;
import com.firstticket.paymentservice.domain.Payment;
import com.firstticket.paymentservice.domain.PaymentRepository;
import com.firstticket.paymentservice.domain.exception.PaymentErrorCode;
import com.firstticket.paymentservice.domain.exception.PaymentException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
            .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        if (!payment.getUserId().equals(userId)) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_FORBIDDEN);
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

    @Transactional(readOnly = true)
    public Page<PaymentResult> getAllPayments(Pageable pageable) {
        return paymentRepository.findAll(pageable)
            .map(PaymentResult::from);
    }
}
