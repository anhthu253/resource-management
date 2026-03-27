package com.example.springboot.controller;

import com.example.springboot.dto.*;
import com.example.springboot.mapper.BookingMapper;
import com.example.springboot.model.Booking;
import com.example.springboot.service.BookingService;
import com.example.springboot.service.PaymentService;
import com.example.springboot.service.StatusUpdateService;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;


@Slf4j
@RestController
@RequestMapping("/booking")
public class MainController {
    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final BookingMapper bookingMapper;
    private final StatusUpdateService statusUpdateService;
    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    public MainController(BookingService bookingService, PaymentService paymentService, BookingMapper bookingMapper, StatusUpdateService statusUpdateService) {
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.bookingMapper = bookingMapper;
        this.statusUpdateService = statusUpdateService;
    }
    @GetMapping("/all-resources")
    public ResponseEntity<?> getAllResources() {
        try{
            return new ResponseEntity<>(this.bookingService.getAllResources(), HttpStatus.OK);
        }
        catch (Exception ex){
            return new ResponseEntity<>("Failed to get resources", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @PostMapping("/available-resources")
    public ResponseEntity<?> getAvailableResource(@RequestBody BookingPeriodDto bookingPeriodDto) {
        try {
            return new ResponseEntity<>(this.bookingService.getAvailableResources(bookingPeriodDto), HttpStatus.OK);
        }
        catch (Exception ex){
            return new ResponseEntity<>("Failed to get available resources", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @PostMapping("/create")
    public ResponseEntity<?> createBooking(@RequestBody BookingDto bookingRequestDto) {
        try{
            Booking bookingRequest = bookingMapper.mapBookingDtoToBooking(bookingRequestDto);
            Booking booking = this.bookingService.createBooking(bookingRequest);
            BookingDto result = this.bookingMapper.mapBookingToBookingDto(booking);
            return new ResponseEntity<>(result, HttpStatus.OK);
        }
        catch(Exception ex){
            return new ResponseEntity<>("Failed to create booking", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @PostMapping("/total-price")
    public ResponseEntity<Double> getTotalPrice(@RequestBody BookingDto bookingDto){
        double totalPrice = this.bookingService.getTotalPricePerBooking(bookingDto);
        return ResponseEntity.status(HttpStatus.OK).body(totalPrice);
    }
    @PostMapping("/update")
    public ResponseEntity<?> updateBooking(@RequestBody Long bookingId) {
            try{
                this.bookingService.updateBooking(bookingId);
                return ResponseEntity.ok().body("Booking update request is successfully created");
            }
            catch (NoSuchElementException e){
                return new ResponseEntity<>("Booking not found", HttpStatus.NOT_FOUND);
            }
            catch(DataIntegrityViolationException e){
                return new ResponseEntity<>("Failed to update booking due to database constraint", HttpStatus.BAD_REQUEST);
            }
            catch(Exception e){
                return new ResponseEntity<>("Server error while updating booking", HttpStatus.INTERNAL_SERVER_ERROR);
            }
    }

    @PostMapping("/create-refund")
    public ResponseEntity<?> createRefund(@RequestBody Long bookingId) {
            return paymentService.createRefund(bookingId);
    }
    @PostMapping("/cancel")
    public ResponseEntity<?> cancelBooking(@RequestBody Long bookingId) {
        try{
            this.bookingService.cancelBooking(bookingId);
            return ResponseEntity.status(HttpStatus.OK).body(null);
        }
        catch (NoSuchElementException e){
            return new ResponseEntity<>("Booking not found", HttpStatus.NOT_FOUND);
        }
        catch(DataIntegrityViolationException e){
            return new ResponseEntity<>("Failed to cancel booking due to database constraint", HttpStatus.BAD_REQUEST);
        }
        catch(Exception e){
            return new ResponseEntity<>("Server error while canceling booking", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/current-booking/{bookingId}")
    public ResponseEntity<?> getCurrentBooking(@PathVariable Long bookingId) {
        Booking booking = bookingService.getCurrentBooking(bookingId);
        return ResponseEntity.ok(bookingMapper.mapBookingToBookingDto(booking));

    }

    @GetMapping("/my-bookings/{userId}")
    public ResponseEntity<List<BookingDto>> getUserBookings(@PathVariable Long userId) {
        List<Booking> bookings = this.bookingService.getMyBookings(userId);
        return new ResponseEntity<>(this.bookingMapper.mapBookingListToBookingDtoList(bookings), HttpStatus.OK);
    }
    @GetMapping("/pending-bookings/{userId}")
    public ResponseEntity<List<BookingDto>> getUserPendingBookings(@PathVariable Long userId) {
        List<Booking> bookings = this.bookingService.getPendingBookings(userId);
        return new ResponseEntity<>(this.bookingMapper.mapBookingListToBookingDtoList(bookings), HttpStatus.OK);
    }
    @PostMapping("/proceed-payment")
    public ResponseEntity<String> proceedPayment(@RequestBody PaymentIntentDto paymentIntentDto) {
        return paymentService.createPaymentIntent(paymentIntentDto);
    }
    @GetMapping(
            path = "/refund/status/{bookingId}",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<StatusUpdateDto> stream(@PathVariable Long bookingId) {
        return statusUpdateService.getStatusStream(bookingId);
    }
    @PostMapping("/webhooks/stripe")
    public ResponseEntity<?> handleStripeWebhook(@RequestBody String payload,
                                                      @RequestHeader("Stripe-Signature") String sigHeader) throws IOException {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            // Signature mismatch → invalid webhook
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        try {
            StripeObject stripeObject = deserializer.deserializeUnsafe();
            paymentService.updatePayment(event.getType(), stripeObject);
            return ResponseEntity.ok().build();
        }
        catch(EventDataObjectDeserializationException e){
            log.error(
                    "Stripe webhook deserialization failed. Event type: {}, Event ID: {}, Error: {}",
                    event.getType(),
                    event.getId(),
                    e.getMessage(),
                    e
            );
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (IllegalArgumentException e) {
            // Payment/Booking not found, or unexpected Stripe object
            log.error("Webhook processing failed: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (DataAccessException e) {
            // DB failure → return 500 so Stripe will retry
            log.error("Database error while updating payment: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            // Catch-all for safety
            log.error("Unexpected error in Stripe webhook: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
