package com.firstticket.paymentservice.infrastructure;


import com.firstticket.paymentservice.domain.Payment;
import com.firstticket.paymentservice.infrastructure.persistence.PaymentJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@EnableJpaAuditing
@ActiveProfiles("test")
class PaymentJpaRepositoryTest {

    @Autowired
    private PaymentJpaRepository paymentJpaRepository;

    private Payment createPayment(UUID bookingId, UUID userId) {
        return Payment.create(
            bookingId,
            userId,
            UUID.randomUUID().toString().replace("-", ""),
            50000
        );
    }

    @Test
    @DisplayName("orderId로 결제 조회")
    void find_by_order_id() {
        // given
        Payment payment = createPayment(UUID.randomUUID(), UUID.randomUUID());
        paymentJpaRepository.save(payment);

        // when
        Optional<Payment> result = paymentJpaRepository.findByOrderId(payment.getOrderId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getOrderId()).isEqualTo(payment.getOrderId());
    }

    @Test
    @DisplayName("bookingId로 결제 조회")
    void find_by_booking_id() {
        // given
        UUID bookingId = UUID.randomUUID();
        Payment payment = createPayment(bookingId, UUID.randomUUID());
        paymentJpaRepository.save(payment);

        // when
        Optional<Payment> result = paymentJpaRepository.findByBookingId(bookingId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getBookingId()).isEqualTo(bookingId);
    }

    @Test
    @DisplayName("userId로 결제 목록 조회")
    void find_all_by_user_id() {
        // given
        UUID userId = UUID.randomUUID();
        Payment payment1 = createPayment(UUID.randomUUID(), userId);
        Payment payment2 = createPayment(UUID.randomUUID(), userId);
        paymentJpaRepository.save(payment1);
        paymentJpaRepository.save(payment2);

        // when
        List<Payment> result = paymentJpaRepository.findAllByUserId(userId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(p -> p.getUserId().equals(userId));
    }

    @Test
    @DisplayName("존재하지 않는 orderId 조회 시 빈 Optional 반환")
    void find_by_order_id_not_found() {
        // when
        Optional<Payment> result = paymentJpaRepository.findByOrderId("notExistOrderId");

        // then
        assertThat(result).isEmpty();
    }
}
