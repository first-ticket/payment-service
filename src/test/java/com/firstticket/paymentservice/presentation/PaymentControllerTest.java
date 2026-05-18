package com.firstticket.paymentservice.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firstticket.paymentservice.application.PaymentCommandService;
import com.firstticket.paymentservice.application.PaymentQueryService;
import com.firstticket.paymentservice.application.dto.result.PaymentResult;
import com.firstticket.paymentservice.domain.PaymentStatus;
import com.firstticket.paymentservice.presentation.dto.request.PaymentConfirmRequest;
import com.firstticket.paymentservice.presentation.dto.request.PaymentCreateRequest;
import com.firstticket.paymentservice.presentation.dto.request.PaymentRefundRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@ExtendWith(RestDocumentationExtension.class)
class PaymentControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private PaymentCommandService paymentCommandService;

    @MockitoBean
    private PaymentQueryService paymentQueryService;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .apply(MockMvcRestDocumentation.documentationConfiguration(restDocumentation)
                .operationPreprocessors()
                .withRequestDefaults(prettyPrint())
                .withResponseDefaults(prettyPrint()))
            .build();
    }

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
    @DisplayName("결제 생성 성공")
    void create_payment_success() throws Exception {
        UUID userId = UUID.randomUUID();
        PaymentResult result = createPaymentResult(userId);
        PaymentCreateRequest request = new PaymentCreateRequest(
            UUID.randomUUID(), userId, 50000
        );

        given(paymentCommandService.createPayment(any())).willReturn(result);

        mockMvc.perform(RestDocumentationRequestBuilders
                .post("/api/v1/payments")
                .header("X-User-Id", userId.toString())
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andDo(document("payment-create",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                    headerWithName("X-User-Id").description("사용자 UUID")
                ),
                requestFields(
                    fieldWithPath("bookingId").description("예매 UUID"),
                    fieldWithPath("userId").description("사용자 UUID"),
                    fieldWithPath("finalAmount").description("결제 금액")
                ),
                responseFields(
                    fieldWithPath("success").description("성공 여부"),
                    fieldWithPath("code").description("응답 코드"),
                    fieldWithPath("message").description("응답 메시지"),
                    fieldWithPath("timestamp").description("응답 시간"),
                    fieldWithPath("data.paymentId").description("결제 UUID"),
                    fieldWithPath("data.userId").description("사용자 UUID"),
                    fieldWithPath("data.orderId").description("주문 ID"),
                    fieldWithPath("data.amount").description("결제 금액"),
                    fieldWithPath("data.status").description("결제 상태"),
                    fieldWithPath("data.requestedAt").description("결제 요청 시간"),
                    fieldWithPath("data.approvedAt").description("결제 승인 시간")
                )
            ));
    }

    @Test
    @DisplayName("토스 결제창 성공")
    void get_payment_page_success() throws Exception {
        mockMvc.perform(RestDocumentationRequestBuilders
                .get("/api/v1/payments/payment-page")
                .param("orderId", "orderId123")
                .param("amount", "50000"))
            .andExpect(status().isOk())
            .andDo(document("payment-page",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                queryParameters(
                    parameterWithName("orderId").description("주문 ID"),
                    parameterWithName("amount").description("결제 금액")
                )
            ));
    }

    @Test
    @DisplayName("결제 승인 성공")
    void confirm_payment_success() throws Exception {
        UUID userId = UUID.randomUUID();
        PaymentResult result = createPaymentResult(userId);
        PaymentConfirmRequest request = new PaymentConfirmRequest(
            "paymentKey123", "orderId123", 50000
        );

        given(paymentCommandService.confirmPayment(any())).willReturn(result);

        mockMvc.perform(RestDocumentationRequestBuilders
                .post("/api/v1/payments/confirm")
                .header("X-User-Id", userId.toString())
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andDo(document("payment-confirm",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestFields(
                    fieldWithPath("paymentKey").description("토스 결제 키"),
                    fieldWithPath("orderId").description("주문 ID"),
                    fieldWithPath("amount").description("결제 금액")
                ),
                responseFields(
                    fieldWithPath("success").description("성공 여부"),
                    fieldWithPath("code").description("응답 코드"),
                    fieldWithPath("message").description("응답 메시지"),
                    fieldWithPath("timestamp").description("응답 시간"),
                    fieldWithPath("data.paymentId").description("결제 UUID"),
                    fieldWithPath("data.userId").description("사용자 UUID"),
                    fieldWithPath("data.orderId").description("주문 ID"),
                    fieldWithPath("data.amount").description("결제 금액"),
                    fieldWithPath("data.status").description("결제 상태"),
                    fieldWithPath("data.requestedAt").description("결제 요청 시간"),
                    fieldWithPath("data.approvedAt").description("결제 승인 시간")
                )
            ));
    }

    @Test
    @DisplayName("결제 승인 리다이렉트 성공")
    void confirm_payment_redirect_success() throws Exception {
        PaymentResult result = new PaymentResult(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "orderId123",
            50000,
            PaymentStatus.SUCCESS,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        given(paymentCommandService.confirmPayment(any())).willReturn(result);

        mockMvc.perform(RestDocumentationRequestBuilders
                .get("/api/v1/payments/confirm-redirect")
                .param("paymentKey", "paymentKey123")
                .param("orderId", "orderId123")
                .param("amount", "50000"))
            .andExpect(status().isOk())
            .andDo(document("payment-confirm-redirect",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                queryParameters(
                    parameterWithName("paymentKey").description("토스 결제 키"),
                    parameterWithName("orderId").description("주문 ID"),
                    parameterWithName("amount").description("결제 금액")
                )
            ));
    }

    @Test
    @DisplayName("결제 실패 화면")
    void fail_payment() throws Exception {
        mockMvc.perform(RestDocumentationRequestBuilders
                .get("/api/v1/payments/fail")
                .param("code", "PAY_PROCESS_CANCELED")
                .param("message", "사용자에 의해 결제가 취소되었습니다")
                .param("orderId", "orderId123"))
            .andExpect(status().isOk())
            .andDo(document("payment-fail",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                queryParameters(
                    parameterWithName("code").description("토스 실패 코드"),
                    parameterWithName("message").description("실패 사유"),
                    parameterWithName("orderId").description("주문 ID")
                )
            ));
    }

    @Test
    @DisplayName("결제 상세 조회 성공")
    void get_payment_success() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        PaymentResult result = createPaymentResult(userId);

        given(paymentQueryService.getPayment(any(), any())).willReturn(result);

        mockMvc.perform(RestDocumentationRequestBuilders
                .get("/api/v1/payments/{paymentId}", paymentId)
                .header("X-User-Id", userId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andDo(document("payment-get",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                    headerWithName("X-User-Id").description("사용자 UUID")
                ),
                pathParameters(
                    parameterWithName("paymentId").description("결제 UUID")
                ),
                responseFields(
                    fieldWithPath("success").description("성공 여부"),
                    fieldWithPath("code").description("응답 코드"),
                    fieldWithPath("message").description("응답 메시지"),
                    fieldWithPath("timestamp").description("응답 시간"),
                    fieldWithPath("data.paymentId").description("결제 UUID"),
                    fieldWithPath("data.userId").description("사용자 UUID"),
                    fieldWithPath("data.orderId").description("주문 ID"),
                    fieldWithPath("data.amount").description("결제 금액"),
                    fieldWithPath("data.status").description("결제 상태"),
                    fieldWithPath("data.requestedAt").description("결제 요청 시간"),
                    fieldWithPath("data.approvedAt").description("결제 승인 시간")
                )
            ));
    }

    @Test
    @DisplayName("본인 결제 목록 조회 성공")
    void get_my_payments_success() throws Exception {
        UUID userId = UUID.randomUUID();
        PaymentResult result = createPaymentResult(userId);

        given(paymentQueryService.getMyPayments(any())).willReturn(List.of(result));

        mockMvc.perform(RestDocumentationRequestBuilders
                .get("/api/v1/payments/me")
                .header("X-User-Id", userId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andDo(document("payment-get-my",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                    headerWithName("X-User-Id").description("사용자 UUID")
                ),
                responseFields(
                    fieldWithPath("success").description("성공 여부"),
                    fieldWithPath("code").description("응답 코드"),
                    fieldWithPath("message").description("응답 메시지"),
                    fieldWithPath("timestamp").description("응답 시간"),
                    fieldWithPath("data[].paymentId").description("결제 UUID"),
                    fieldWithPath("data[].userId").description("사용자 UUID"),
                    fieldWithPath("data[].orderId").description("주문 ID"),
                    fieldWithPath("data[].amount").description("결제 금액"),
                    fieldWithPath("data[].status").description("결제 상태"),
                    fieldWithPath("data[].requestedAt").description("결제 요청 시간"),
                    fieldWithPath("data[].approvedAt").description("결제 승인 시간")
                )
            ));
    }

    @Test
    @DisplayName("전체 결제 목록 조회 성공 (어드민)")
    void get_all_payments_success() throws Exception {
        UUID userId = UUID.randomUUID();
        PaymentResult result = createPaymentResult(userId);
        Page<PaymentResult> page = new PageImpl<>(List.of(result));

        given(paymentQueryService.getAllPayments(any())).willReturn(page);

        mockMvc.perform(RestDocumentationRequestBuilders
                .get("/api/v1/payments/admin")
                .header("X-User-Id", userId.toString())
                .header("X-User-Role", "ADMIN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andDo(document("payment-get-all",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                    headerWithName("X-User-Id").description("사용자 UUID"),
                    headerWithName("X-User-Role").description("사용자 권한 (ADMIN)")
                )
            ));
    }

    @Test
    @DisplayName("환불 성공")
    void refund_payment_success() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        PaymentResult result = createPaymentResult(userId);
        PaymentRefundRequest request = new PaymentRefundRequest("테스트 환불");

        given(paymentCommandService.refundPayment(any())).willReturn(result);

        mockMvc.perform(RestDocumentationRequestBuilders
                .post("/api/v1/payments/{paymentId}/refund", paymentId)
                .header("X-User-Id", userId.toString())
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andDo(document("payment-refund",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                    headerWithName("X-User-Id").description("사용자 UUID")
                ),
                pathParameters(
                    parameterWithName("paymentId").description("결제 UUID")
                ),
                requestFields(
                    fieldWithPath("cancelReason").description("환불 사유")
                ),
                responseFields(
                    fieldWithPath("success").description("성공 여부"),
                    fieldWithPath("code").description("응답 코드"),
                    fieldWithPath("message").description("응답 메시지"),
                    fieldWithPath("timestamp").description("응답 시간"),
                    fieldWithPath("data.paymentId").description("결제 UUID"),
                    fieldWithPath("data.userId").description("사용자 UUID"),
                    fieldWithPath("data.orderId").description("주문 ID"),
                    fieldWithPath("data.amount").description("결제 금액"),
                    fieldWithPath("data.status").description("결제 상태"),
                    fieldWithPath("data.requestedAt").description("결제 요청 시간"),
                    fieldWithPath("data.approvedAt").description("결제 승인 시간")
                )
            ));
    }
}
