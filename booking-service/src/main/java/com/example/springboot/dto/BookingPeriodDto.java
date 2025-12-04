package com.example.springboot.dto;

import java.time.LocalDateTime;

public record BookingPeriodDto(LocalDateTime startedAt, LocalDateTime endedAt) {
}
