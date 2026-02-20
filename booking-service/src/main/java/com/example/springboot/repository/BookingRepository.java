package com.example.springboot.repository;

import com.example.springboot.model.Booking;
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
    @Query("SELECT b from Booking b where b.startedAt <= :ended and b.endedAt > :started and (b.status = BookingStatus.CONFIRMED or b.status = PENDING_CONFIRMATION )")
    List<Booking> getBookings(LocalDateTime started, LocalDateTime ended);
    @Query("Select b from Booking b where b.user.id = :userId and b.status = BookingStatus.CONFIRMED")
    List<Booking> getBookingsByUserId(Long userId);
    Optional<Booking> findByPayment(Payment payment);
    @Transactional
    @Modifying
    @Query("UPDATE Booking b SET b.status = BookingStatus.CANCELED WHERE b.bookingId = :bookingId")
    int cancelBookingById(@Param("bookingId") long bookingId);
    @Transactional
    @Modifying
    @Query("UPDATE Booking b SET b.status = BookingStatus.MODIFIED WHERE b.bookingId = :bookingId")
    int modifyBookingById(@Param("bookingId") long bookingId);
}
