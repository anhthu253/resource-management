package com.example.springboot.model;

import jakarta.persistence.*;

import java.util.UUID;
@Entity
@Table(name="payment")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;
    @OneToOne
    @JoinColumn(name="booking_id")
    private Booking booking;
    private double amount;
    private String provider;
    @Enumerated(EnumType.STRING)
    private Status status;

}
