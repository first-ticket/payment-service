package com.firstticket.paymentservice.presentation;

import com.firstticket.paymentservice.application.PaymentCommandService;
import com.firstticket.paymentservice.presentation.dto.request.PaymentCreateRequest;
import com.firstticket.paymentservice.presentation.dto.response.PaymentResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
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

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
        @RequestBody @Valid PaymentCreateRequest request) {
        PaymentResponse response = PaymentResponse.from(
            paymentCommandService.createPayment(request.toCommand())
        );
        return ResponseEntity.ok(response);
    }

    /*
    //결제 생성을 payment-service에서 임의로 호출하는 테스트과정
    //http://localhost:8085/internal/v1/payments/payment-page?bookingId=xxx&userId=xxx&amount=xxx 로
    //bookingId 와 userId는 임시로 생성해서 사용 (bookingId는 중복 불가)

    @GetMapping("/payment-page")
    public ResponseEntity<String> getPaymentPage(
        @RequestParam UUID bookingId,
        @RequestParam UUID userId,
        @Positive @RequestParam Integer amount) {

        PaymentResult result = paymentCommandService.createPayment(
            new CreatePaymentCommand(bookingId, userId, amount)
        );

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
                  successUrl: "http://localhost:8085/api/v1/payments/confirm-redirect",
                  failUrl: "http://localhost:8085/fail"
                });
              </script>
            </body>
            </html>
            """.formatted(result.amount(), result.orderId());

        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(html);
    }
     */

    //Booking Service 연동 후 결제 생성 API 호출하면 orderId 반환 후
    //http://localhost:8085/internal/v1/payments/payment-page?orderId=xxx&amount=50000 로 토스 결제창 연동
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
                  successUrl: "http://localhost:8084/api/v1/payments/confirm-redirect",
                  failUrl: "http://localhost:8084/fail"
                });
              </script>
            </body>
            </html>
            """.formatted(amount, orderId);

        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(html);
    }
}
