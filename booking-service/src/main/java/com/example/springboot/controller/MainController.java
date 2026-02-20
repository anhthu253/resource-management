package com.example.springboot.controller;

import com.example.springboot.dto.*;
import com.example.springboot.mapper.BookingMapper;
import com.example.springboot.model.Booking;
import com.example.springboot.model.PaymentStatus;
import com.example.springboot.service.BookingService;
import com.example.springboot.service.StripeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.*;
import com.stripe.net.ApiResource;
import com.stripe.net.Webhook;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@RestController
@RequestMapping("/booking")
public class MainController {
    private final BookingService bookingService;
    private final StripeService stripeService;
    private final BookingMapper bookingMapper;
    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    public MainController(BookingService bookingService, StripeService stripeService, BookingMapper bookingMapper) {
        this.bookingService = bookingService;
        this.stripeService = stripeService;
        this.bookingMapper = bookingMapper;
    }
    @GetMapping("/all-resources")
    public ResponseEntity<List<ResourceDto>> getAllResources() {
        return new ResponseEntity<>(this.bookingService.getAllResources(), HttpStatus.OK);
    }
    @PostMapping("/available-resources")
    public ResponseEntity<List<ResourceDto>> getAvailableResource(@RequestBody BookingPeriodDto bookingPeriodDto) {
        return new ResponseEntity<>(this.bookingService.getAvailableResources(bookingPeriodDto), HttpStatus.OK);
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateBooking(@RequestBody BookingRequestDto bookingRequestDto) {
        if(bookingRequestDto.getBookingId() != null){
            try{
                int nr = this.bookingService.updateBooking(bookingRequestDto.getBookingId(), "modified");
                if(nr == 0){
                    String error = "Unsuccessfully modify booking with booking Id "+bookingRequestDto.getBookingId();
                    return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
            catch (Exception ex){
                return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
            }

            try{
                int refundStatusCode = bookingService.createRefund(bookingRequestDto.getBookingId());
                if (refundStatusCode >= 400 && refundStatusCode < 500) {
                    return ResponseEntity
                            .status(HttpStatus.BAD_REQUEST)
                            .body("Refund rejected by Stripe");
                }
                if (refundStatusCode >= 500) {
                    return ResponseEntity
                            .status(HttpStatus.BAD_GATEWAY)
                            .body("Stripe service unavailable. Please try again later.");
                }
            }
            catch (Exception ex){
                return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_GATEWAY);
            }
        }
        Booking bookingRequest = bookingMapper.mapBookingRequestToBookingEntity(bookingRequestDto);
        BookingResponseDto result = this.bookingService.createBooking(bookingRequest);
        return new ResponseEntity<>(result, HttpStatus.OK);

    }
    @PostMapping("/cancel")
    public ResponseEntity<Void> cancelBooking(@RequestBody Long bookingId) {
            if(this.bookingService.updateBooking(bookingId, "canceled")==0)
                return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
            return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/current-booking/{bookingId}")
    public ResponseEntity<BookingDto> getCurrentBooking(@PathVariable Long bookingId) {
        Booking booking = bookingService.getCurrentBooking(bookingId);
        return ResponseEntity.ok(bookingMapper.mapBookingToBookingDto(booking));
    }

    @GetMapping("/my-bookings/{userId}")
    public ResponseEntity<List<BookingDto>> getUserBookings(@PathVariable Long userId) {
        var myBookings = this.bookingService.getMyBookings(userId);
        return new ResponseEntity<>(this.bookingMapper.mapBookingListToBookingDtoList(myBookings), HttpStatus.OK);
    }
    @PostMapping("/proceed-payment")
    public ResponseEntity<String> proceedPayment(@RequestBody PaymentIntentDto paymentIntentDto) {
        try {
            return bookingService.createPaymentIntent(paymentIntentDto);
        } catch (Exception ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        }

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
            HttpStatus status = bookingService.updatePayment(event.getType(), stripeObject);
            return new ResponseEntity<>(status);
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
/*
    @PostMapping("/webhooks/stripe")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            // Verify the signature
            Event event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
            String paymentIntentId = null;
            String chargeId = null;
            Long amount = null;

            EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
            if (deserializer.getObject().isPresent())
                bookingService.updatePayment(event.getType(), deserializer.getObject().get());
            else {
                //when Stripe Java SDK is older than Stripe API version
                String rawJson = deserializer.getRawJson();
                if (rawJson != null) {
                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        JsonNode root = mapper.readTree(rawJson);
                        JsonNode objectNode = root.path("data").path("object");
                        paymentIntentId = objectNode.path("id").asText(null);
                        chargeId = objectNode.path("latest_charge").asText(null);
                    } catch (Exception ex) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid webhook payload");
                    }
                }
            }
            return ResponseEntity.ok("Received");

        } catch (SignatureVerificationException e) {
            // Invalid signature
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook signature verification failed");
        }
    }
    */


    /*
     * @PostMapping("/confirm")
     * ResponseEntity<BookingStatus> confirmBooking (@RequestBody BookingConfirmDto
     * bookingConfirmDto){
     * Booking boookingConfirm =
     * bookingRequestMapper.mapBookingRequestToBookingEntity(bookingConfirmDto);
     * BookingResponseDto result =
     * this.bookingService.createBooking(boookingConfirm);
     * return new ResponseEntity<>(result, HttpStatus.OK);
     * }
     */
}
