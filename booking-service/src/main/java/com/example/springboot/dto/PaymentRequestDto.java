package com.example.springboot.dto;

public record PaymentRequestDto(Long bookingId, String paymentIntentId) {
}
