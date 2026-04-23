package com.firstticket.paymentservice.presentation;

import com.firstticket.common.response.ApiResponse;
import com.firstticket.common.response.CommonSuccessCode;
import com.firstticket.paymentservice.application.PaymentCommandService;
import com.firstticket.paymentservice.presentation.dto.request.PaymentCreateRequest;
import com.firstticket.paymentservice.presentation.dto.response.PaymentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/payments")
public class PaymentInternalController {

    private final PaymentCommandService paymentCommandService;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
        @RequestBody @Valid PaymentCreateRequest request) {
        PaymentResponse response = PaymentResponse.from(
            paymentCommandService.createPayment(request.toCommand())
        );
        return ApiResponse.success(CommonSuccessCode.CREATED, response);
        //결제 성공 코드 추후 구현
    }
}
