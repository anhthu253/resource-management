package com.example.springboot.dto;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
public class ResourceDto {
        private Long resourceId;
        private String resourceName;
        private double basePrice;
        private PriceUnit priceUnit;

}
