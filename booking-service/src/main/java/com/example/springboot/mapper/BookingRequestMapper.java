package com.example.springboot.mapper;

import com.example.springboot.dto.BookingRequestDto;
import com.example.springboot.model.Booking;
import com.example.springboot.model.User;
import com.example.springboot.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
@Component
public class BookingRequestMapper {
    private final UserRepository userRepository;
    public BookingRequestMapper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Booking mapBookingRequestToBookingEntity(BookingRequestDto bookingRequest){

            Booking booking = new Booking();
            booking.setResourceIds(bookingRequest.getResourceIds());
            booking.setStartedAt(bookingRequest.getStartedAt());
            booking.setEndedAt(bookingRequest.getEndedAt());
            booking.setTotalPrice(bookingRequest.getTotalPrice());
            User user = this.userRepository.findById(bookingRequest.getUserId()).orElseThrow(() -> new RuntimeException("User not found"));
            booking.setUser(user);
            return booking;
    }
}
