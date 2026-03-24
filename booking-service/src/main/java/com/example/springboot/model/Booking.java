package com.example.springboot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="booking")
@Getter
@Setter
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookingId;
    private String bookingNumber;
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    private List<BookingLog> logs = new ArrayList<>();
    @ManyToOne
    @JoinColumn(name="user_id")
    private User user;
    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL)
    private Payment payment;
    @ElementCollection
    @CollectionTable(
            name = "booking_resources",
            joinColumns = @JoinColumn(name = "booking_id")
    )
    @Column(name = "resource_id")
    private List<Long> resourceIds;
    @Enumerated(EnumType.STRING)
    private BookingStatus bookingStatus;
    @Enumerated(EnumType.STRING)
    private ModificationStatus modificationStatus;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime createdAt;
    private LocalDateTime expiredAt;
    private BigDecimal totalPrice;
}
