package com.example.springboot.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingEvent {
    private String eventId;
    private String bookingId;
    private String bookingNumber;
    private List<String> resourceNames;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal amount;
    private MQEventType bookingEventType;
    private String userEmail;
    private String userFullName;
}
