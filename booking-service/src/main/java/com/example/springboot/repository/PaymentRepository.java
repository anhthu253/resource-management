package com.example.springboot.repository;

import com.example.springboot.model.Booking;
import com.example.springboot.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByBooking(Booking booking);

    Optional<Payment> findByPaymentIntentId(String paymentIntentId);
    Optional<Payment> findByChargeId(String chargeId);
    @Query("Select p from Payment p where p.booking.bookingId = :bookingId")
    Optional<Payment> findByBookingId(@Param("bookingId") long bookingId);
}
