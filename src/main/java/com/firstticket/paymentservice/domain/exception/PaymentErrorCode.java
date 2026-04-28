package com.firstticket.paymentservice.domain.exception;

import com.firstticket.common.response.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode implements ErrorCode {

    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "결제를 찾을 수 없습니다."),
    PAYMENT_ALREADY_SUCCESS(HttpStatus.BAD_REQUEST, "이미 승인된 결제입니다."),
    PAYMENT_INVALID_STATUS(HttpStatus.BAD_REQUEST, "현재 상태에서 처리할 수 없습니다."),
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "결제 금액이 일치하지 않습니다."),
    PAYMENT_CONFIRM_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "토스 결제 승인에 실패했습니다."),
    PAYMENT_CANCEL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "토스 결제 취소에 실패했습니다."),
    PAYMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "본인의 결제만 접근할 수 있습니다.");

    private final HttpStatus status;
    private final String message;
}
