package com.example.springboot.dto;

import com.example.springboot.model.BookingStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
public class BookingRequestDto {
    private List<Long> resourceIds;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private String purpose;
    private Long userId;
    private BigDecimal totalPrice;
}
