package com.example.springboot.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
@Getter
@Setter
public class PaymentIntentDto {
    private Long bookingId;
    private Long paymentId;
    private String paymentMethodId;
}
