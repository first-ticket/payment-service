package com.firstticket.paymentservice.presentation;

import com.firstticket.common.response.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentSuccessCode implements SuccessCode {

    PAYMENT_CREATED(HttpStatus.CREATED, "결제가 생성되었습니다."),
    PAYMENT_CONFIRMED(HttpStatus.OK, "결제가 승인되었습니다."),
    PAYMENT_REFUNDED(HttpStatus.OK, "결제가 환불되었습니다."),
    PAYMENT_FOUND(HttpStatus.OK, "결제를 조회했습니다."),
    PAYMENT_LIST_FOUND(HttpStatus.OK, "결제 목록을 조회했습니다.");

    private final HttpStatus status;
    private final String message;
}
