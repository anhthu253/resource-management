package com.example.springboot.dto;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
@Getter
public class PriceRequest {
    private List<Long> resourceIds;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
}
