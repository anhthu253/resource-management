package com.example.springboot.controller;

import com.example.springboot.dto.*;
import com.example.springboot.mapper.BookingMapper;
import com.example.springboot.model.Booking;
import com.example.springboot.service.BookingService;
import com.example.springboot.service.StripeService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

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

    @PostMapping("/available-resources")
    public ResponseEntity<List<ResourceDto>> getAvailableResource(@RequestBody BookingPeriodDto bookingPeriodDto) {
        return new ResponseEntity<>(this.bookingService.getAvailableResources(bookingPeriodDto), HttpStatus.OK);
    }

    @PostMapping("/create")
    public ResponseEntity<BookingResponseDto> createBooking(@RequestBody BookingRequestDto bookingRequestDto) {
        Booking bookingRequest = bookingMapper.mapBookingRequestToBookingEntity(bookingRequestDto);
        BookingResponseDto result = this.bookingService.createBooking(bookingRequest);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/current-booking/{bookingId}")
    public ResponseEntity<BookingDto> getCurrentBooking(@PathVariable Long bookingId) {
        Booking booking = bookingService.getCurrentBooking(bookingId);
        return ResponseEntity.ok(bookingMapper.mapBookingToBookingDto(booking));
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
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            // Verify the signature
            Event event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
            String paymentIntentId = null;

            EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();

            if (deserializer.getObject().isPresent()) {
                PaymentIntent paymentIntent = (PaymentIntent) deserializer.getObject().get();
                paymentIntentId = paymentIntent.getId();
            } else {
                String rawJson = deserializer.getRawJson();
                if (rawJson != null) {
                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        PaymentIntentResponseDto paymentIntentResponseDto = mapper.readValue(rawJson,
                                PaymentIntentResponseDto.class);
                        paymentIntentId = paymentIntentResponseDto.id();
                    } catch (Exception ex) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid webhook payload");
                    }
                }
            }
            bookingService.updatePaymentStatus(event.getType(), paymentIntentId);
            return ResponseEntity.ok("Received");

        } catch (SignatureVerificationException e) {
            // Invalid signature
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook signature verification failed");
        }
    }

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
