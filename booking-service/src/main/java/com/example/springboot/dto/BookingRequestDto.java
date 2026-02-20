package com.example.springboot.dto;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
public class BookingRequestDto {
    private Long bookingId;
    private List<Long> resourceIds;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Long userId;
    private BigDecimal totalPrice;
}
