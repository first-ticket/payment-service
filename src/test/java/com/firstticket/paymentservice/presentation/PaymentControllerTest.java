package com.firstticket.paymentservice.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firstticket.paymentservice.application.PaymentCommandService;
import com.firstticket.paymentservice.application.PaymentQueryService;
import com.firstticket.paymentservice.application.dto.result.PaymentResult;
import com.firstticket.paymentservice.domain.PaymentStatus;
import com.firstticket.paymentservice.presentation.dto.request.PaymentRefundRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentCommandService paymentCommandService;

    @MockitoBean
    private PaymentQueryService paymentQueryService;

    private PaymentResult createPaymentResult(UUID userId) {
        return new PaymentResult(
            UUID.randomUUID(),
            userId,
            "orderId123",
            50000,
            PaymentStatus.SUCCESS,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("결제 상세 조회 성공")
    void get_payment_success() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        PaymentResult result = createPaymentResult(userId);

        given(paymentQueryService.getPayment(any(), any())).willReturn(result);

        mockMvc.perform(get("/api/v1/payments/{paymentId}", paymentId)
                .header("X-User-Id", userId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("본인 결제 목록 조회 성공")
    void get_my_payments_success() throws Exception {
        UUID userId = UUID.randomUUID();
        PaymentResult result = createPaymentResult(userId);

        given(paymentQueryService.getMyPayments(any())).willReturn(List.of(result));

        mockMvc.perform(get("/api/v1/payments/me")
                .header("X-User-Id", userId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("환불 성공")
    void refund_payment_success() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        PaymentResult result = createPaymentResult(userId);
        PaymentRefundRequest request = new PaymentRefundRequest("테스트 환불");

        given(paymentCommandService.refundPayment(any())).willReturn(result);

        mockMvc.perform(post("/api/v1/payments/{paymentId}/refund", paymentId)
                .header("X-User-Id", userId.toString())
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
}
