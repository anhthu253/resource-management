package com.example.springboot.model;

public enum PaymentStatus {
    AWAITING_CUSTOMER_ACTION,
    PROCESSING,
    SUCCEEDED_PENDING_WEBHOOK,
    SUCCEEDED,
    FAILED,
    REFUNDED,
    REFUND_PENDING,
    REFUND_FAILED
}
