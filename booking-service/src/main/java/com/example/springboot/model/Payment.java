package com.example.springboot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name="payment")
@Setter
@Getter
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;
    @OneToOne
    @JoinColumn(name="booking_id")
    private Booking booking;
    private BigDecimal amount;
    private Currency currency;
    private String provider;
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

}
