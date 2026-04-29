CREATE TABLE p_payment
(
    id               UUID         NOT NULL,
    booking_id       UUID         NOT NULL,
    user_id          UUID         NOT NULL,
    order_id         VARCHAR(64)  NOT NULL,
    payment_key      VARCHAR(200),
    final_amount     INTEGER      NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    requested_at     TIMESTAMP    NOT NULL,
    approved_at      TIMESTAMP,
    retry_expired_at TIMESTAMP,
    created_at       TIMESTAMP    NOT NULL,
    updated_at       TIMESTAMP,
    deleted_at       TIMESTAMP,

    CONSTRAINT pk_payment PRIMARY KEY (id),
    CONSTRAINT uq_payment_booking_id UNIQUE (booking_id),
    CONSTRAINT uq_payment_order_id UNIQUE (order_id)
);

CREATE TABLE p_payments_history
(
    id                UUID         NOT NULL,
    payment_id        UUID         NOT NULL,
    status            VARCHAR(20)  NOT NULL,
    reason            VARCHAR(255),
    toss_raw_response TEXT,
    created_at        TIMESTAMP    NOT NULL,
    updated_at        TIMESTAMP,
    deleted_at        TIMESTAMP,

    CONSTRAINT pk_payments_history PRIMARY KEY (id),
    CONSTRAINT fk_payments_history_payment
        FOREIGN KEY (payment_id)
            REFERENCES p_payment (id)
);
