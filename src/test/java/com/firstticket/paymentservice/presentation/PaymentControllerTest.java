package com.firstticket.paymentservice.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firstticket.paymentservice.application.PaymentCommandService;
import com.firstticket.paymentservice.application.PaymentQueryService;
import com.firstticket.paymentservice.application.dto.result.PaymentResult;
import com.firstticket.paymentservice.domain.PaymentStatus;
import com.firstticket.paymentservice.presentation.dto.request.PaymentRefundRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
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
