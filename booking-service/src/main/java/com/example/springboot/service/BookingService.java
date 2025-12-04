package com.example.springboot.service;

import com.example.springboot.dto.*;
import com.example.springboot.model.*;
import com.example.springboot.repository.BookingRepository;
import com.example.springboot.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingService {
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final WebClient resourceWebClient;
    private final StripeService stripeService;
    private final ResourceService resourceService;

    public BookingService(BookingRepository bookingRepository,
                          PaymentRepository paymentRepository,
                          @Qualifier("resourceWebClient") WebClient resourceWebClient,
                          StripeService stripeService,
                          ResourceService resourceService
    ) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.resourceWebClient = resourceWebClient;
        this.stripeService = stripeService;
        this.resourceService = resourceService;
    }

    public BookingResponseDto createBooking(Booking bookingRequest) {
        bookingRequest.setStatus(BookingStatus.PENDING_CONFIRMATION);
        Booking booking = this.bookingRepository.save(bookingRequest);
        Payment payment = createPayment(booking);
        return new BookingResponseDto(booking.getBookingId(), payment.getPaymentId(), payment.getStatus());
    }

    public Booking getCurrentBooking(Long bookingId){
        return bookingRepository.findById(bookingId).orElseThrow();
    }
    public ResponseEntity<String> createPaymentIntent(PaymentIntentDto paymentIntentDto) throws Exception {
        HttpResponse<String> paymentIntentResponse = stripeService.postPaymentIntents(paymentIntentDto);
        if (paymentIntentResponse.statusCode() == 200) {
            String jsonRes = paymentIntentResponse.body();
            var mapper = new ObjectMapper();
            PaymentIntentResponseDto paymentIntent = mapper.readValue(jsonRes, PaymentIntentResponseDto.class);
            Payment payment = paymentRepository.findById(paymentIntentDto.getPaymentId()).orElseThrow();
            payment.setPaymentIntentId(paymentIntent.id());
            payment.setStatus(PaymentStatus.PROCESSING);
            paymentRepository.save(payment);
            Booking booking = bookingRepository.findById(paymentIntentDto.getBookingId()).orElseThrow();
            booking.setStatus(BookingStatus.PAYMENT_PENDING);
            return new ResponseEntity<>(paymentIntent.client_secret(), HttpStatus.OK);
        } else return new ResponseEntity<>("payment is not proceeded", HttpStatus.NOT_IMPLEMENTED);
    }

    public Payment createPayment(Booking booking) {
        var payment = new Payment();
        payment.setBooking(booking);
        payment.setStatus(PaymentStatus.AWAITING_CUSTOMER_ACTION);
        payment.setAmount(booking.getTotalPrice());
        payment.setCurrency(Currency.EUR);
        return this.paymentRepository.save(payment);
    }

    public BookingStatus updatePaymentStatus(String eventType, String paymentIntentId) {
        Payment payment = paymentRepository.findByPaymentIntentId(paymentIntentId).orElseThrow();
        Booking booking = bookingRepository.findByPayment(payment).orElseThrow();

        switch (eventType) {
            case "payment_intent.succeeded":
                payment.setStatus(PaymentStatus.SUCCEEDED);
                paymentRepository.save(payment);
                booking.setStatus(BookingStatus.CONFIRMED);
                bookingRepository.save(booking);
                break;
            case "payment_intent.payment_failed":
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                booking.setStatus(BookingStatus.PAYMENT_FAILED);
                bookingRepository.save(booking);
                break;
            default:
                break;
        }
        return booking.getStatus();
    }
    public List<ResourceDto> getAvailableResources(BookingPeriodDto bookingPeriodDto) {
        List<ResourceDto> allResource = resourceService.getAllResources().block();
        List<Long> bookedResourceIds = this.bookingRepository.getBookings(bookingPeriodDto.startedAt(), bookingPeriodDto.endedAt())
                .stream().flatMap(booking -> booking.getResourceIds().stream())
                .distinct()
                .collect(Collectors.toList());
        if(bookedResourceIds.size() == 0) return allResource;
        return allResource.stream()
                .filter(resource -> !bookedResourceIds.contains(resource.getResourceId()))
                .collect(Collectors.toList());
    }

}
