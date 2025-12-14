package com.example.springboot.repository;

import com.example.springboot.model.Booking;
import com.example.springboot.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Query("SELECT b from Booking b where b.startedAt <= :ended and b.endedAt > :started and (b.status = BookingStatus.CONFIRMED or b.status = PENDING_CONFIRMATION )")
    List<Booking> getBookings(LocalDateTime started, LocalDateTime ended);

    Optional<Booking> findByPayment(Payment payment);
}
