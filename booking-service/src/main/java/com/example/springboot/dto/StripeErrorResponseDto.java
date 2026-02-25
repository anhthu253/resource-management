package com.example.springboot.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StripeErrorResponseDto {
    private StripeErrorDto error;
}
