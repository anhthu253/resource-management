package com.example.springboot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentIntentResponseDto (String id, Long amount, String client_secret) {
}
