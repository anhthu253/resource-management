package com.example.springboot.repository;

import com.example.springboot.model.Booking;
import com.example.springboot.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment,Long> {
    Optional<Payment> findByBooking(Booking booking);
    Optional<Payment> findByPaymentIntentId(String paymentIntentId);
}
