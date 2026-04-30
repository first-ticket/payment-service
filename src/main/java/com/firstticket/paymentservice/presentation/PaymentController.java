package com.firstticket.paymentservice.presentation;

import com.firstticket.common.response.ApiResponse;
import com.firstticket.common.response.CommonSuccessCode;
import com.firstticket.common.web.AuthContext;
import com.firstticket.common.web.UserRole;
import com.firstticket.paymentservice.application.PaymentCommandService;
import com.firstticket.paymentservice.application.PaymentQueryService;
import com.firstticket.paymentservice.application.dto.command.ConfirmPaymentCommand;
import com.firstticket.paymentservice.domain.exception.PaymentErrorCode;
import com.firstticket.paymentservice.domain.exception.PaymentException;
import com.firstticket.paymentservice.presentation.dto.request.PaymentConfirmRequest;
import com.firstticket.paymentservice.presentation.dto.request.PaymentRefundRequest;
import com.firstticket.paymentservice.presentation.dto.response.PaymentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentCommandService paymentCommandService;
    private final PaymentQueryService paymentQueryService;

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmPayment(
        @RequestBody @Valid PaymentConfirmRequest request) {
        PaymentResponse response = PaymentResponse.from(
            paymentCommandService.confirmPayment(request.toCommand())
        );
        return ApiResponse.success(PaymentSuccessCode.PAYMENT_CONFIRMED, response);
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

    // 결제 상세 조회
    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
        @PathVariable UUID paymentId) {
        UUID userId = AuthContext.getUserId();
        PaymentResponse response = PaymentResponse.from(
            paymentQueryService.getPayment(paymentId, userId)
        );
        return ApiResponse.success(PaymentSuccessCode.PAYMENT_FOUND, response);
    }

    // 본인 결제 목록 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getMyPayments() {
        UUID userId = AuthContext.getUserId();
        List<PaymentResponse> response = paymentQueryService.getMyPayments(userId)
            .stream()
            .map(PaymentResponse::from)
            .toList();
        return ApiResponse.success(PaymentSuccessCode.PAYMENT_LIST_FOUND, response);
    }

    //전체 결제 목록 조회 (ADMIN)
    @GetMapping("/admin")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getAllPayments(
        Pageable pageable) {

        UserRole role = AuthContext.getRole();
        if (UserRole.ADMIN != role) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_FORBIDDEN);
        }

        Page<PaymentResponse> response = paymentQueryService.getAllPayments(pageable)
            .map(PaymentResponse::from);

        return ApiResponse.success(PaymentSuccessCode.PAYMENT_LIST_FOUND, response);
    }

    // 환불
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(
        @PathVariable UUID paymentId,
        @RequestBody @Valid PaymentRefundRequest request) {
        UUID userId = AuthContext.getUserId();
        PaymentResponse response = PaymentResponse.from(
            paymentCommandService.refundPayment(request.toCommand(paymentId, userId))
        );
        return ApiResponse.success(PaymentSuccessCode.PAYMENT_REFUNDED, response);
    }
}
