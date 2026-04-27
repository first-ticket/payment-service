package com.firstticket.paymentservice.infrastructure.external;

import com.firstticket.paymentservice.domain.service.TossPaymentsPort;
import com.firstticket.paymentservice.domain.service.dto.TossCancelResult;
import com.firstticket.paymentservice.domain.service.dto.TossConfirmResult;
import com.firstticket.paymentservice.infrastructure.external.dto.TossCancelRequest;
import com.firstticket.paymentservice.infrastructure.external.dto.TossCancelResponse;
import com.firstticket.paymentservice.infrastructure.external.dto.TossConfirmRequest;
import com.firstticket.paymentservice.infrastructure.external.dto.TossConfirmResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@Component
@RequiredArgsConstructor
public class TossPaymentsAdapter implements TossPaymentsPort {

    private final RestTemplate restTemplate;

    @Value("${toss.secret-key}")
    private String secretKey;

    private static final String TOSS_CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";
    private static final String TOSS_CANCEL_URL = "https://api.tosspayments.com/v1/payments/{paymentKey}/cancel";

    @Override
    public TossConfirmResult confirm(String paymentKey, String orderId, Integer amount) {
        HttpHeaders headers = createHeaders();
        TossConfirmRequest request = new TossConfirmRequest(paymentKey, orderId, amount);
        HttpEntity<TossConfirmRequest> entity = new HttpEntity<>(request, headers);

        TossConfirmResponse response = restTemplate.postForObject(
            TOSS_CONFIRM_URL,
            entity,
            TossConfirmResponse.class
        );

        return response.toResult();
    }

    @Override
    public TossCancelResult cancel(String paymentKey, String cancelReason) {
        HttpHeaders headers = createHeaders();
        TossCancelRequest request = new TossCancelRequest(cancelReason);
        HttpEntity<TossCancelRequest> entity = new HttpEntity<>(request, headers);

        TossCancelResponse response = restTemplate.postForObject(
            TOSS_CANCEL_URL,
            entity,
            TossCancelResponse.class,
            paymentKey
        );

        return response.toResult();
    }

    private HttpHeaders createHeaders() {
        String encoded = Base64.getEncoder().encodeToString((secretKey + ":").getBytes());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + encoded);
        return headers;
    }
}
