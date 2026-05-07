package com.firstticket.paymentservice.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@Configuration
public class KafkaConfig {

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        // 실패 메시지 DLT 토픽으로 발행
        DeadLetterPublishingRecoverer recoverer =
            new DeadLetterPublishingRecoverer(kafkaTemplate);

        // 1초 간격으로 3회 재시도 후 DLQ로
        FixedBackOff backOff = new FixedBackOff(1000L, 3L);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        // 재시도 시 로그
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) ->
            log.warn("Kafka 재시도 - topic: {}, attempt: {}, error: {}",
                record.topic(), deliveryAttempt, ex.getMessage())
        );

        return errorHandler;
    }
}
