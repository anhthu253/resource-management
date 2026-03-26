package com.example.springboot.dto;

import com.example.springboot.model.PaymentStatus;
import com.example.springboot.model.RefundStatus;
import lombok.Getter;

public record RefundStatusUpdateDto(long bookingId, RefundStatus status, String message){}

