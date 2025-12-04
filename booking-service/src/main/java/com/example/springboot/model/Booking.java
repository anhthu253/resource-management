package com.example.springboot.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.time.LocalDate;
@Entity
@Table(name="booking")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookingId;
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    private List<BookingLog> logs = new ArrayList<>();
    @ManyToOne
    @JoinColumn(name="user_id")
    private User user;
    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL)
    private Payment payment;
    private Long resourceId;
    @Enumerated(EnumType.STRING)
    private Status status;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalPrice;
}
