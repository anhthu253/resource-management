package com.example.springboot.dto;
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
    private BigDecimal totalPrice;
}
