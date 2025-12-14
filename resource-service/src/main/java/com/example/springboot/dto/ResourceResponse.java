package com.example.springboot.dto;

import com.example.springboot.model.PriceUnit;

public record ResourceResponse(Long resourceId, String resourceName, Double basePrice, PriceUnit priceUnit) {
}
