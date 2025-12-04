package com.example.springboot.dto;

import com.example.springboot.model.BookingStatus;
import com.example.springboot.model.PaymentStatus;

import java.math.BigDecimal;

public record BookingResponseDto (Long bookingId, Long paymentId, PaymentStatus paymentStatus){}