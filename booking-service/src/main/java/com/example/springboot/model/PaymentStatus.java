package com.example.springboot.model;

public enum PaymentStatus {
    AWAITING_CUSTOMER_ACTION,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    REFUND_PENDING,
    REFUNDED,
    REFUND_FAILED
}
