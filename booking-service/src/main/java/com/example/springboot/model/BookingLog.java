package com.example.springboot.model;
import jakarta.persistence.*;

import java.util.UUID;
@Entity
@Table(name="booking_log")
public class BookingLog {
    @Id
    @GeneratedValue()
    private Long logId;
    private String status;
    @ManyToOne
    @JoinColumn(name="booking_id")
    private Booking booking;
}
