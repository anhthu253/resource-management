package com.example.springboot.service;

import com.example.springboot.dto.*;
import com.example.springboot.model.*;
import com.example.springboot.repository.BookingRepository;
import com.example.springboot.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.StripeObject;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.stereotype.Service;

import java.net.http.HttpResponse;
import java.util.*;

import java.util.stream.Collectors;

@Service
public class BookingService {
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final ResourceService resourceService;

    public BookingService(BookingRepository bookingRepository,
                          PaymentRepository paymentRepository,
                          PaymentService paymentService, StripeService stripeService,
                          ResourceService resourceService,
                          RefundService refundService) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
        this.resourceService = resourceService;
    }
    @Transactional
    public BookingResponseDto createBooking(Booking booking) {
        booking.setBookingStatus(BookingStatus.PENDING_CONFIRMATION);
        booking.setModificationStatus(ModificationStatus.NONE);
        booking = this.bookingRepository.save(booking);
        Payment payment = paymentService.createPayment(booking);
        return new BookingResponseDto(booking.getBookingId(), payment.getPaymentId(), payment.getPaymentStatus());
    }
    public void updateBooking(long bookingId) throws Exception{
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        booking.setModificationStatus(ModificationStatus.MODIFY_PENDING);
        bookingRepository.save(booking);
    }
    public void cancelBooking (long bookingId) throws Exception {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        booking.setBookingStatus(BookingStatus.CANCEL_PENDING);
        bookingRepository.save(booking);
    }
    public List<Booking> getMyBookings(long userId){
        return bookingRepository.getBookingsByUserId(userId);
    }
    public Booking getCurrentBooking(Long bookingId) {
        return bookingRepository.findById(bookingId).orElseThrow();
    }
    public List<ResourceDto> getAvailableResources(BookingPeriodDto bookingPeriodDto) {
        List<ResourceDto> allResource = getAllResources();
        List<Long> bookedResourceIds = this.bookingRepository
                .getBookings(bookingPeriodDto.startedAt(), bookingPeriodDto.endedAt())
                .stream().flatMap(booking -> booking.getResourceIds().stream())
                .distinct()
                .collect(Collectors.toList());
        if (bookedResourceIds.size() == 0)
            return allResource;
        return allResource.stream()
                .filter(resource -> !bookedResourceIds.contains(resource.getResourceId()))
                .collect(Collectors.toList());
    }
    public List<ResourceDto> getAllResources(){
        return resourceService.getAllResources().block();
    }

}
