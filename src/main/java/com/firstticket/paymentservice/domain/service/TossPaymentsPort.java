package com.firstticket.paymentservice.domain.service;

import com.firstticket.paymentservice.domain.service.dto.TossCancelResult;
import com.firstticket.paymentservice.domain.service.dto.TossConfirmResult;

public interface TossPaymentsPort {

    TossConfirmResult confirm(String paymentKey, String orderId, Integer amount);

    TossCancelResult cancel(String paymentKey, String cancelReason);
}
