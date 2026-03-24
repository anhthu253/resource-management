package com.example.springboot.dto;

import com.example.springboot.model.BookingStatus;
import com.example.springboot.model.ModificationStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;
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
    private Date startedAt;
    private Date endedAt;
    private BigDecimal totalPrice;
    private Long userId;
}
