package com.example.springboot.repository;

import com.example.springboot.model.Booking;
import com.example.springboot.model.BookingStatus;
import com.example.springboot.model.Payment;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Query("SELECT b from Booking b where b.startedAt <= :ended and b.endedAt > :started and b.bookingStatus in (BookingStatus.CONFIRMED, BookingStatus.PENDING_CONFIRMATION)")
    List<Booking> getBookings(LocalDateTime started, LocalDateTime ended);
    @Query("Select b from Booking b where b.user.id = :userId and b.bookingStatus = BookingStatus.CONFIRMED")
    List<Booking> getBookingsByUserId(Long userId);
    Optional<Booking> findByPayment(Payment payment);
    List<Booking> findByBookingStatusAndExpiredAtBefore(BookingStatus bookingStatus, LocalDateTime time);
    @Query("SELECT nextval('booking_number_seq')")
    Long getBookingSequence();
}
