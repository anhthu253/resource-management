package com.example.springboot.dto;

import com.example.springboot.model.BookingStatus;
import com.example.springboot.model.ModificationStatus;
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
    private String bookingNumber;
    private List<ResourceDto> resources;
    private Long paymentId;
    private BookingStatus bookingStatus;
    private ModificationStatus modificationStatus;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private BigDecimal totalPrice;
    private Long userId;
}
