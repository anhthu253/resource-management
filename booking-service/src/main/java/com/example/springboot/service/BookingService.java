package com.example.springboot.service;

import com.example.springboot.dto.*;
import com.example.springboot.model.*;
import com.example.springboot.repository.BookingRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;
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
    public Booking createBooking(Booking booking) throws Exception {
        Booking previousBooking = null;
        if(booking.getBookingGroupId() == null){
            booking.setBookingGroupId(UUID.randomUUID());
        }
        else{
            previousBooking = bookingRepository.findPreviousBooking(booking.getBookingGroupId()).orElse(null);
        }
        booking.setBookingStatus(BookingStatus.PENDING_CONFIRMATION);
        booking.setCreatedAt(LocalDateTime.now());
        booking.setExpiredAt(LocalDateTime.now().plusMinutes(15)); //users have 15 minutes to pay
        booking.setBookingNumber(generateBookingNumber());
        try{
            Payment payment = paymentService.createPayment(booking);
            booking.setPayment(payment);
            booking = this.bookingRepository.save(booking);
            try{
                if(previousBooking == null) {
                    bookingEventPublisher.publishBookingEvent(booking, null, MQEventType.BOOKING_CREATED);
                }
                else{
                    bookingEventPublisher.publishBookingEvent(booking, previousBooking, MQEventType.BOOKING_MODIFIED);
                }
            }
            catch(Exception ex){
                log.error("Unsuccessfully deliver booking created/modified event to RabbitMQ.");
            }
            return booking;

        }
        catch (Exception e){
            if(previousBooking != null){
                previousBooking.setBookingStatus(BookingStatus.CONFIRMED); //reverse to active booking
            }
            try{
                if(previousBooking == null) {
                    bookingEventPublisher.publishBookingEvent(booking, null, MQEventType.BOOKING_FAILED);
                }
                else {
                    bookingEventPublisher.publishBookingEvent(booking, previousBooking, MQEventType.BOOKING_MODIFY_FAILED);
                }
            }
            catch(Exception ex){
                log.error("Unsuccessfully deliver failed booking created/modified event to RabbitMQ.");
            }
            throw new Exception(e);
        }

    }
    public void updateBooking(long bookingId) throws Exception{
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        try{
            booking.setBookingStatus(BookingStatus.REPLACED);
            booking.setModifiedAt(LocalDateTime.now());
            bookingRepository.save(booking);
        }
        catch (Exception e){
            throw new Exception(e);
        }
    }
    public void cancelBooking (long bookingId) throws Exception {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        booking.setBookingStatus(BookingStatus.CANCELED);
        try{
            bookingRepository.save(booking);
            try{
                bookingEventPublisher.publishBookingEvent(booking, null, MQEventType.BOOKING_CANCELED);
            }
            catch(Exception ex){
                log.error("Unsuccessfully deliver booking canceled event to RabbitMQ.");
            }
        }
        catch (Exception e){
            booking.setBookingStatus(BookingStatus.CONFIRMED); //reverse to active booking
            try{
                bookingEventPublisher.publishBookingEvent(booking, null, MQEventType.BOOKING_CANCEL_FAILED);
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
    public double getTotalPricePerBooking(BookingDto booking){
        Instant startedInstant = booking.getStartedAt().toInstant();
        Instant endedInstant = booking.getEndedAt().toInstant();
        long diffSeconds = ChronoUnit.SECONDS.between(startedInstant, endedInstant);
        return booking.getResources().stream()
                .map(resource -> getBasePricePerSecond(diffSeconds, resource.getBasePrice(), resource.getPriceUnit()))
                .reduce(0.0, Double::sum);
    }
    public List<ResourceDto> getAvailableResources(BookingPeriodDto bookingPeriodDto) throws Exception {
        List<ResourceDto> allResource = getAllResources();
        LocalDateTime startedAt = bookingPeriodDto.startedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime endedAt = bookingPeriodDto.endedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        List<Long> bookedResourceIds = this.bookingRepository
                .getBookings(startedAt, endedAt)
                .stream().flatMap(booking -> booking.getResourceIds().stream())
                .distinct()
                .collect(Collectors.toList());
        if (bookedResourceIds.size() == 0)
            return allResource;
        return allResource.stream()
                .filter(resource -> !bookedResourceIds.contains(resource.getResourceId()))
                .collect(Collectors.toList());
    }
    public List<ResourceDto> getAllResources() throws Exception {
        return resourceService.getAllResources();
    }

    private String generateBookingNumber(){
        Long seq = bookingRepository.getBookingSequence();
        return "BK-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + String.format("%06d", seq);
    }

    private double getBasePricePerSecond(long periodInsSeconds, double basePrice, PriceUnit priceUnit){

        switch(priceUnit){
            case hourly -> {
                return periodInsSeconds * basePrice / (60 * 60);
            }
            case daily -> {
                return periodInsSeconds* basePrice / ( 60 * 60 * 24);
            }
            default -> { //price per booking
                return basePrice;
            }
        }

    }
}
