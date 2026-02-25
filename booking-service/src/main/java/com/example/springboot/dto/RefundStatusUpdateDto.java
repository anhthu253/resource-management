package com.example.springboot.dto;

import com.example.springboot.model.PaymentStatus;
import lombok.Getter;

public record RefundStatusUpdateDto(long bookingId, PaymentStatus status, String message){}

