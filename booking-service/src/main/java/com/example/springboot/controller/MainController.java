package com.example.springboot.controller;

import com.example.springboot.dto.BookingPeriodDto;
import com.example.springboot.dto.BookingRequestDto;
import com.example.springboot.dto.BookingResponseDto;
import com.example.springboot.dto.ResourceDto;
import com.example.springboot.mapper.BookingRequestMapper;
import com.example.springboot.model.Booking;
import com.example.springboot.service.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/booking")
public class MainController {
    private final BookingService bookingService;
    private final BookingRequestMapper bookingRequestMapper;
    public MainController(BookingService bookingService, BookingRequestMapper bookingRequestMapper) {
        this.bookingService = bookingService;
        this.bookingRequestMapper = bookingRequestMapper;
    }
    @PostMapping("/available-resources")
    ResponseEntity<List<ResourceDto>> getAvailableResource(@RequestBody BookingPeriodDto bookingPeriodDto){
        return new ResponseEntity<>(this.bookingService.getAvailableResources(bookingPeriodDto), HttpStatus.OK);
    }
    @PostMapping("/create")
    ResponseEntity<BookingResponseDto> createBooking(@RequestBody BookingRequestDto bookingRequestDto){
        Booking bookingRequest = bookingRequestMapper.mapBookingRequestToBookingEntity(bookingRequestDto);
        BookingResponseDto result = this.bookingService.createBooking(bookingRequest);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
