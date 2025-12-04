package com.example.springboot.model;

import jakarta.persistence.*;
import lombok.Getter;

import java.util.UUID;

    @Entity
    @Table(name="resource")
    @Getter
    public class Resource {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long resourceId;
        private Long bookingId;
        private String name;
        private String type;
        private String description;
        private int capacity;
        private double basePrice;
        private String metadata;
    }
