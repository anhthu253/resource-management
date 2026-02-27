package com.example.springboot.service;

import com.example.springboot.dto.*;
import com.example.springboot.model.*;
import com.example.springboot.repository.BookingRepository;

import jakarta.transaction.Transactional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import java.util.stream.Collectors;
@Slf4j
@Service
public class BookingService {
    private final BookingRepository bookingRepository;
    private final PaymentService paymentService;
    private final ResourceService resourceService;
    private final BookingEventPublisher bookingEventPublisher;

    public BookingService(BookingRepository bookingRepository,
                          PaymentService paymentService,
                          ResourceService resourceService, BookingEventPublisher bookingEventPublisher) {
        this.bookingRepository = bookingRepository;
        this.paymentService = paymentService;
        this.resourceService = resourceService;
        this.bookingEventPublisher = bookingEventPublisher;
    }
    @Transactional
    public BookingResponseDto createBooking(Booking booking) {
        booking.setBookingStatus(BookingStatus.PENDING_CONFIRMATION);
        booking.setModificationStatus(ModificationStatus.NONE);
        booking.setCreatedAt(LocalDateTime.now());
        booking.setExpiredAt(LocalDateTime.now().plusMinutes(15)); //users have 15 minutes to pay
        booking.setBookingNumber(generateBookingNumber());
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
        booking.setBookingStatus(BookingStatus.CANCELED);
        try{
            bookingRepository.save(booking);
            try{
                bookingEventPublisher.publishBookingEvent(booking, MQEventType.BOOKING_CANCELED);
            }
            catch(Exception ex){
                log.error("Unsuccessfully deliver booking canceled event to RabbitMQ.");
            }
        }
        catch (Exception e){
            booking.setBookingStatus(BookingStatus.CONFIRMED); //reverse to active booking
            try{
                bookingEventPublisher.publishBookingEvent(booking, MQEventType.BOOKING_CANCEL_FAILED);
            }
            catch(Exception ex){
                log.error("Unsuccessfully deliver failed cancel event to RabbitMQ.");
            }
        }

    }
    public List<Booking> getMyBookings(long userId){
        return bookingRepository.getBookingsByUserId(userId);
    }
    public Booking getCurrentBooking(Long bookingId) {
        return bookingRepository.findById(bookingId).orElseThrow();
    }
    public List<Booking> getPendingBookings(long userId) {
        return this.bookingRepository.getPendingBookingsByUserId(userId);
    }
    public void expirePendingBookings() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> expiredBookings =
                bookingRepository.findByBookingStatusAndExpiredAtBefore(
                        BookingStatus.PENDING_CONFIRMATION,
                        now
                );

        for (Booking booking : expiredBookings) {
            booking.setBookingStatus(BookingStatus.EXPIRED);
        }

        bookingRepository.saveAll(expiredBookings);
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

    private String generateBookingNumber(){
        Long seq = bookingRepository.getBookingSequence();
        return "BK-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + String.format("%06d", seq);
    }


}
