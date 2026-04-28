package com.firstticket.paymentservice.domain.exception;

import com.firstticket.common.exception.BusinessException;

public class PaymentException extends BusinessException {

    public PaymentException(PaymentErrorCode errorCode) {
        super(errorCode);
    }
}
