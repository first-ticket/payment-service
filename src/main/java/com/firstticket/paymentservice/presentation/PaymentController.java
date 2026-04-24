package com.firstticket.paymentservice.presentation;

import com.firstticket.common.response.ApiResponse;
import com.firstticket.common.response.CommonSuccessCode;
import com.firstticket.paymentservice.application.PaymentCommandService;
import com.firstticket.paymentservice.presentation.dto.request.PaymentConfirmRequest;
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
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentCommandService paymentCommandService;

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmPayment(
        @RequestBody @Valid PaymentConfirmRequest request) {
        PaymentResponse response = PaymentResponse.from(
            paymentCommandService.confirmPayment(request.toCommand())
        );
        return ApiResponse.success(CommonSuccessCode.OK, response);
    }
}
