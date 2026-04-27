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
    /**
     * 테스트용 엔드포인트 - Booking 서비스 연동 완료 후 제거 예정
     * @deprecated 테스트 완료 후 제거 예정
     */
    @Deprecated
    @GetMapping(value = "/confirm-redirect", produces = "application/json;charset=UTF-8")
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
