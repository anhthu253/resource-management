package com.example.springboot.service;

import com.example.springboot.dto.BookingRequest;
import com.example.springboot.dto.BookingResult;
import com.example.springboot.model.Booking;
import com.example.springboot.repository.BookingRepository;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class BookingService {
    private final BookingRepository bookingRepository;
    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public BookingResult createBooking (BookingRequest bookingRequest){
        List<Long> resourceIds = bookingRequest.getResourcesIds();
        for (Long resourceId:resourceIds) {
            //if at least a booking found for one resource
            if (this.bookingRepository.getBookings(resourceId, bookingRequest.getStartedAt(), bookingRequest.getEndedAt()).stream().count() > 0)
                return BookingResult.Failure;
        }
        this.bookingRepository.save(booking);
        return BookingResult.Success;
    }


}
