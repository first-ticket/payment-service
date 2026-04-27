package com.firstticket.paymentservice.presentation;

import com.firstticket.common.response.ApiResponse;
import com.firstticket.common.response.CommonSuccessCode;
import com.firstticket.paymentservice.application.PaymentCommandService;
import com.firstticket.paymentservice.application.dto.command.ConfirmPaymentCommand;
import com.firstticket.paymentservice.presentation.dto.request.PaymentConfirmRequest;
import com.firstticket.paymentservice.presentation.dto.response.PaymentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/confirm-redirect")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmPaymentRedirect(
        @RequestParam String paymentKey,
        @RequestParam String orderId,
        @RequestParam Integer amount) {
        PaymentResponse response = PaymentResponse.from(
            paymentCommandService.confirmPayment(
                new ConfirmPaymentCommand(paymentKey, orderId, amount)
            )
        );
        return ApiResponse.success(CommonSuccessCode.OK, response);
    }
}
