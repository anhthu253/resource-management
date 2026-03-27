package com.example.springboot.model;

public enum RefundStatus implements Status {
    NONE,
    NOT_ELIGIBLE,
    PENDING,
    REFUNDED,
    FAILED,
}
