package com.firstticket.paymentservice.presentation;

import com.firstticket.paymentservice.application.PaymentCommandService;
import com.firstticket.paymentservice.presentation.dto.request.PaymentCreateRequest;
import com.firstticket.paymentservice.presentation.dto.response.PaymentResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@ConditionalOnProperty(
    prefix = "feature.internal-payments",
    name = "enabled",
    havingValue = "true"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/payments")
public class PaymentInternalController {

    private final PaymentCommandService paymentCommandService;

    @Value("${payment.success-url}")
    private String successUrl;

    @Value("${payment.fail-url}")
    private String failUrl;

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
        @RequestBody @Valid PaymentCreateRequest request) {
        PaymentResponse response = PaymentResponse.from(
            paymentCommandService.createPayment(request.toCommand())
        );
        return ResponseEntity.ok(response);
    }

    //Booking Service 연동 후 결제 생성 API 호출하면 orderId 반환 후
    //http://localhost:8084/internal/v1/payments/payment-page?orderId=xxx&amount=50000 로 토스 결제창 연동
    @GetMapping("/payment-page")
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
}
