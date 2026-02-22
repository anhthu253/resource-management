package com.example.springboot.dto;

import com.example.springboot.model.BookingStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
@Setter
@Getter
public class BookingDto {
    private Long bookingId;
    private List<ResourceDto> resources;
    @Enumerated(EnumType.STRING)
    private BookingStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private BigDecimal totalPrice;
}
