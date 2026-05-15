package com.firstticket.paymentservice.presentation;

import com.firstticket.common.response.ApiResponse;
import com.firstticket.common.response.CommonSuccessCode;
import com.firstticket.common.web.AuthContext;
import com.firstticket.common.web.UserRole;
import com.firstticket.paymentservice.application.PaymentCommandService;
import com.firstticket.paymentservice.application.PaymentQueryService;
import com.firstticket.paymentservice.application.dto.command.ConfirmPaymentCommand;
import com.firstticket.paymentservice.application.dto.result.PaymentResult;
import com.firstticket.paymentservice.domain.PaymentStatus;
import com.firstticket.paymentservice.domain.exception.PaymentErrorCode;
import com.firstticket.paymentservice.domain.exception.PaymentException;
import com.firstticket.paymentservice.presentation.dto.request.PaymentConfirmRequest;
import com.firstticket.paymentservice.presentation.dto.request.PaymentCreateRequest;
import com.firstticket.paymentservice.presentation.dto.request.PaymentRefundRequest;
import com.firstticket.paymentservice.presentation.dto.response.PaymentResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
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

    @Value("${payment.success-url}")
    private String successUrl;

    @Value("${payment.fail-url}")
    private String failUrl;

    // 결제 생성 (Booking 서비스 연동)
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
        @RequestBody @Valid PaymentCreateRequest request) {
        PaymentResponse response = PaymentResponse.from(
            paymentCommandService.createPayment(request.toCommand())
        );
        return ApiResponse.success(PaymentSuccessCode.PAYMENT_CREATED, response);
    }

    // 토스 결제창
    @GetMapping(value = "/payment-page", produces = "application/json;charset=UTF-8")
    public ResponseEntity<String> getPaymentPage(
        @RequestParam String orderId,
        @Positive @RequestParam Integer amount) {

        String html = """
        <!DOCTYPE html>
        <html lang="ko">
        <head>
          <meta charset="UTF-8" />
          <script src="https://js.tosspayments.com/v1/payment"></script>
        </head>
        <body>
          <script>
            const tossPayments = TossPayments("test_ck_QbgMGZzorzmyQnGjmOXkVl5E1em4");
            tossPayments.requestPayment("카드", {
              amount: %d,
              orderId: "%s",
              orderName: "First Ticket 예매",
              customerName: "김토스",
              successUrl: "%s",
              failUrl: "%s"
            });
          </script>
        </body>
        </html>
        """.formatted(amount, orderId, successUrl, failUrl);

        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(html);
    }

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmPayment(
        @RequestBody @Valid PaymentConfirmRequest request) {

        PaymentResult result = paymentCommandService.confirmPayment(request.toCommand());

        // FAILED면 실패 응답 반환
        if (result.status() == PaymentStatus.FAILED) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_CONFIRM_FAILED);
        }

        return ApiResponse.success(PaymentSuccessCode.PAYMENT_CONFIRMED, PaymentResponse.from(result));
    }

    @GetMapping("/fail")
    public ResponseEntity<String> fail(
        @RequestParam String code,
        @RequestParam String message,
        @RequestParam String orderId) {
        return ResponseEntity.ok(
            "결제가 실패하였습니다.\n실패 사유: " + message + "\n오류 코드: " + code
        );
    }

    @GetMapping(value = "/confirm-redirect", produces = "text/html;charset=UTF-8")
    public ResponseEntity<String> confirmPaymentRedirect(
        @RequestParam String paymentKey,
        @RequestParam String orderId,
        @RequestParam Integer amount) {

        PaymentResult result = paymentCommandService.confirmPayment(
            new ConfirmPaymentCommand(paymentKey, orderId, amount)
        );

        String html = """
        <!DOCTYPE html>
        <html lang="ko">
        <head><meta charset="UTF-8" /><title>결제 완료</title></head>
        <body>
          <h2>결제가 성공하였습니다! 🎉</h2>
          <p>주문 ID: %s</p>
          <p>결제 금액: %,d원</p>
          <p>결제 상태: %s</p>
          <p>결제 시간: %s</p>
        </body>
        </html>
        """.formatted(
            result.orderId(),
            result.amount(),
            result.status(),
            result.approvedAt()
        );

        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(html);
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
