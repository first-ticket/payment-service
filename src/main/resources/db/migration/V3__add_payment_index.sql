CREATE INDEX idx_payment_user_id ON p_payment (user_id);
CREATE INDEX idx_payment_status_requested_at ON p_payment (status, requested_at);
CREATE INDEX idx_payments_history_payment_id ON p_payments_history (payment_id);
