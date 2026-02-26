package com.example.springboot.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BookingExpirationScheduler {
    private final BookingService bookingService;

    public BookingExpirationScheduler(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Scheduled(fixedRate = 60000) // every 1 minute
    public void expireBookings() {
        bookingService.expirePendingBookings();
    }
}
