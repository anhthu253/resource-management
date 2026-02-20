package com.example.springboot.service;

import com.example.springboot.dto.*;
import com.example.springboot.model.*;
import com.example.springboot.repository.BookingRepository;
import com.example.springboot.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.StripeObject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
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
            ResourceService resourceService) {
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

    public Booking getCurrentBooking(Long bookingId) {
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
        } else
            return new ResponseEntity<>("payment is not proceeded", HttpStatus.NOT_IMPLEMENTED);
    }

    public Payment createPayment(Booking booking) {
        var payment = new Payment();
        payment.setBooking(booking);
        payment.setStatus(PaymentStatus.AWAITING_CUSTOMER_ACTION);
        payment.setAmount(booking.getTotalPrice());
        payment.setCurrency(Currency.EUR);
        return this.paymentRepository.save(payment);
    }
    public HttpStatus updatePayment(String eventType, StripeObject stripeObject) {
        Payment payment;
        Booking booking;
        var status = HttpStatus.OK;

        switch (eventType) {
            // PaymentIntent events
            case "payment_intent.succeeded" -> {
                PaymentIntent paymentIntent = (PaymentIntent) stripeObject;
                String paymentIntentId = paymentIntent.getId();
                String chargeId = paymentIntent.getLatestCharge();

                payment = paymentRepository.findByPaymentIntentId(paymentIntentId)
                        .orElseThrow(() -> new IllegalArgumentException("Payment not found for PaymentIntent: " + paymentIntentId));
                booking = bookingRepository.findByPayment(payment)
                        .orElseThrow(() -> new IllegalArgumentException("Booking not found for Payment: " + payment.getPaymentId()));

                payment.setChargeId(chargeId);
                payment.setStatus(PaymentStatus.SUCCEEDED);
                booking.setStatus(BookingStatus.CONFIRMED);
                status = HttpStatus.OK;
            }

            case "payment_intent.payment_failed" -> {
                PaymentIntent paymentIntent = (PaymentIntent) stripeObject;
                String paymentIntentId = paymentIntent.getId();

                payment = paymentRepository.findByPaymentIntentId(paymentIntentId)
                        .orElseThrow(() -> new IllegalArgumentException("Payment not found for PaymentIntent: " + paymentIntentId));
                booking = bookingRepository.findByPayment(payment)
                        .orElseThrow(() -> new IllegalArgumentException("Booking not found for Payment: " + payment.getPaymentId()));

                payment.setStatus(PaymentStatus.FAILED);
                booking.setStatus(BookingStatus.PAYMENT_FAILED);
                status = HttpStatus.INTERNAL_SERVER_ERROR;
            }

            // Refund events
            case "refund.succeeded" -> {
                Refund refund = (Refund) stripeObject;
                String chargeId = refund.getCharge();
                Long amount = refund.getAmount();

                payment = paymentRepository.findByChargeId(chargeId)
                        .orElseThrow(() -> new IllegalArgumentException("Payment not found for Charge: " + chargeId));
                booking = bookingRepository.findByPayment(payment)
                        .orElseThrow(() -> new IllegalArgumentException("Booking not found for Payment: " + payment.getPaymentId()));

                payment.setStatus(PaymentStatus.REFUNDED);
                payment.setRefundedAmount(amount);
                booking.setStatus(BookingStatus.CANCELED); // or MODIFIED depending on business rule
                status = HttpStatus.OK;
            }

            case "refund.failed" -> {
                Refund refund = (Refund) stripeObject;
                String chargeId = refund.getCharge();
                Long amount = refund.getAmount();

                payment = paymentRepository.findByChargeId(chargeId)
                        .orElseThrow(() -> new IllegalArgumentException("Payment not found for Charge: " + chargeId));
                booking = bookingRepository.findByPayment(payment)
                        .orElseThrow(() -> new IllegalArgumentException("Booking not found for Payment: " + payment.getPaymentId()));

                payment.setStatus(PaymentStatus.REFUND_FAILED);
                booking.setStatus(BookingStatus.CANCELED); // keep booking cancelled; payment failed, financial issue
                status = HttpStatus.INTERNAL_SERVER_ERROR;
            }

            default -> throw new IllegalArgumentException("Unhandled Stripe event type: " + eventType);
        }
        // Save updates
        paymentRepository.save(payment);
        if (booking != null) bookingRepository.save(booking);
        return status;
    }


    public int createRefund(long bookingId) throws Exception {
        Payment payment = paymentRepository.findByBookingId(bookingId).orElseThrow();
        HttpResponse<String> refundResponse = stripeService.postPaymentRefund(payment.getChargeId());
        if (refundResponse.statusCode() == 200) {
            payment.setStatus(PaymentStatus.REFUND_PENDING);
        }
        return refundResponse.statusCode();
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

    public List<Booking> getMyBookings(long userId){
        return bookingRepository.getBookingsByUserId(userId);
    }

    public int updateBooking(long bookingId, String bookingMode){
        bookingRepository.findById(bookingId).orElseThrow(()-> new IllegalArgumentException("Booking with booking ID "+bookingId +" not found"));
        if(bookingMode.toLowerCase() == "canceled")
            return this.bookingRepository.cancelBookingById(bookingId);
        else
            return this.bookingRepository.modifyBookingById(bookingId);

    }

}
