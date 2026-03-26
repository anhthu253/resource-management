package com.example.springboot.service;

import com.example.springboot.dto.PaymentIntentDto;
import com.example.springboot.dto.PaymentIntentResponseDto;
import com.example.springboot.dto.RefundStatusUpdateDto;
import com.example.springboot.dto.StripeErrorResponseDto;
import com.example.springboot.model.*;
import com.example.springboot.repository.BookingRepository;
import com.example.springboot.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.model.Charge;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.StripeObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.http.HttpResponse;
import java.sql.Ref;
import java.time.LocalDateTime;
import java.util.Optional;
@Slf4j
@Service
public class PaymentService {
    public final PaymentRepository paymentRepository;
    public final BookingRepository bookingRepository;
    public final BookingEventPublisher bookingEventPublisher;
    private final StripeService stripeService;
    private final RefundService refundService;
    @Value("${refund.retry.after}")
    private long defautRetryAfter;

    public PaymentService(PaymentRepository paymentRepository, BookingRepository bookingRepository, BookingEventPublisher bookingEventPublisher, StripeService stripeService, RefundService refundService) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.bookingEventPublisher = bookingEventPublisher;
        this.stripeService = stripeService;
        this.refundService = refundService;
    }

    public Payment createPayment(Booking booking) {
        var payment = new Payment();
        payment.setBooking(booking);
        payment.setPaymentStatus(PaymentStatus.AWAITING_CUSTOMER_ACTION);
        payment.setRefundStatus(RefundStatus.NONE);
        payment.setAmount(booking.getTotalPrice());
        payment.setCurrency(Currency.EUR);
        return this.paymentRepository.save(payment);
    }
    public ResponseEntity<String> createPaymentIntent(PaymentIntentDto paymentIntentDto) {
        try {
            HttpResponse<String> paymentIntentResponse = stripeService.postPaymentIntents(paymentIntentDto);
            HttpStatus status = HttpStatus.valueOf(paymentIntentResponse.statusCode());
            if (status.is2xxSuccessful()) {
                String jsonRes = paymentIntentResponse.body();
                var mapper = new ObjectMapper();
                PaymentIntentResponseDto paymentIntent = mapper.readValue(jsonRes, PaymentIntentResponseDto.class);
                Payment payment = paymentRepository.findById(paymentIntentDto.getPaymentId()).orElseThrow();
                payment.setPaymentIntentId(paymentIntent.id());
                payment.setPaymentStatus(PaymentStatus.PROCESSING);
                paymentRepository.save(payment);
                return new ResponseEntity<>(paymentIntent.client_secret(), HttpStatus.OK);
            } else if (status.is4xxClientError())
                return new ResponseEntity<>("Invalid payment info", HttpStatus.BAD_REQUEST);
            else
                return new ResponseEntity<>("Payment is not processed. Please try again later.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        catch(Exception e){
            return new ResponseEntity<>("Payment is not processed. Please try again later.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    public void updatePayment(String eventType, StripeObject stripeObject) {
        switch (eventType) {
            // PaymentIntent events
            case "payment_intent.succeeded" -> {
                PaymentIntent paymentIntent = (PaymentIntent) stripeObject;
                String paymentIntentId = paymentIntent.getId();
                String chargeId = paymentIntent.getLatestCharge();

                Payment payment = paymentRepository.findByPaymentIntentId(paymentIntentId)
                        .orElseThrow(() -> new IllegalArgumentException("Payment not found for PaymentIntent: " + paymentIntentId));
                Booking booking = bookingRepository.findByPayment(payment)
                        .orElseThrow(() -> new IllegalArgumentException("Booking not found for Payment: " + payment.getPaymentId()));

                payment.setChargeId(chargeId);
                payment.setPaymentStatus(PaymentStatus.SUCCEEDED);
                paymentRepository.save(payment);
                booking.setBookingStatus(BookingStatus.CONFIRMED);
                bookingRepository.save(booking);
                try{
                    bookingEventPublisher.publishBookingEvent(booking, MQEventType.BOOKING_CREATED);
                }
                catch(Exception e){
                    log.error("Unsucessfully publish created booking event to RabbitMQ");
                }
            }

            case "payment_intent.payment_failed" -> {
                PaymentIntent paymentIntent = (PaymentIntent) stripeObject;
                String paymentIntentId = paymentIntent.getId();

                Payment payment = paymentRepository.findByPaymentIntentId(paymentIntentId)
                        .orElseThrow(() -> new IllegalArgumentException("Payment not found for PaymentIntent: " + paymentIntentId));
                Booking booking = bookingRepository.findByPayment(payment)
                        .orElseThrow(() -> new IllegalArgumentException("Booking not found for Payment: " + payment.getPaymentId()));

                payment.setPaymentStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                try{
                    bookingEventPublisher.publishBookingEvent(booking, MQEventType.BOOKING_FAILED);
                }
                catch(Exception e){
                    log.error("Unsucessfully publish failed booking event to RabbitMQ");
                }
            }
            case "refund.created", "refund.updated", "refund.failed" -> {Refund refund = (Refund) stripeObject;
                proceedRefundEvent(eventType,refund);
            }

            // Refund events
           case "charge.refunded" -> {
                Charge charge = (Charge) stripeObject;
                String chargeId = charge.getId();
                Long amount = charge.getAmount();

                Payment payment = paymentRepository.findByChargeId(chargeId)
                        .orElseThrow(() -> new IllegalArgumentException("Payment not found for Charge: " + chargeId));
                Booking booking = bookingRepository.findByPayment(payment)
                        .orElseThrow(() -> new IllegalArgumentException("Booking not found for Payment: " + payment.getPaymentId()));

                payment.setRefundedAmount(amount);
                payment.setRefundStatus(RefundStatus.REFUNDED);
                paymentRepository.save(payment);
                refundService.complete(booking.getBookingId());
                try{
                    bookingEventPublisher.publishBookingEvent(booking, MQEventType.BOOKING_REFUNDED);
                }
                catch(Exception e){
                    log.error("Unsucessfully publish booking modified event to RabbitMQ");
                }
            }
        }
    }

    private void proceedRefundEvent(String eventType, Refund refund){
        String chargeId = refund.getCharge();
        Payment payment = paymentRepository.findByChargeId(chargeId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for Charge: " + chargeId));
        Booking booking = bookingRepository.findByPayment(payment)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found for Payment: " + payment.getPaymentId()));
        payment.setRefundStatus(RefundStatus.INITIATED);
        RefundStatusUpdateDto update = new RefundStatusUpdateDto(booking.getBookingId(), RefundStatus.INITIATED,"Refund request for this booking is successfully created.");;
        switch(refund.getStatus()){
            case "pending" -> {
                update = new RefundStatusUpdateDto(booking.getBookingId(), RefundStatus.PENDING,"Refund is being processed… please wait.");
                refundService.notifyUpdate(update);
            }
            case "succeeded" -> {
                payment.setRefundStatus(RefundStatus.REFUNDED);
                paymentRepository.save(payment);
                refundService.complete(booking.getBookingId());
            }
            case "failed" -> {
                payment.setRefundStatus(RefundStatus.FAILED);
                payment.setRefundFailureReason(refund.getFailureReason());
                paymentRepository.save(payment);
                refundService.complete(booking.getBookingId());
                if("refund.failed".equals(eventType)){
                    try{
                        bookingEventPublisher.publishBookingEvent(booking, MQEventType.BOOKING_REFUND_FAILED);
                    }
                    catch(Exception e){
                        log.error("Unsucessfully publish refund failed event to RabbitMQ");
                    }
                }
            }
        }
    }

    public ResponseEntity<?> createRefund(long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        Payment payment = paymentRepository.findByBookingId(bookingId).orElseThrow();
        try {
            payment.setRefundStatus(RefundStatus.PENDING);
            paymentRepository.save(payment);
            HttpResponse<String> refundResponse = stripeService.postPaymentRefund(bookingId, payment.getChargeId());
            HttpStatus status = HttpStatus.valueOf(refundResponse.statusCode());
            if (status.is2xxSuccessful()) {
                return new ResponseEntity<>("Refund request is successfully created.",HttpStatus.OK);
            } else if (status.is5xxServerError()) {;
                this.refundService.processRefundAsync(booking, payment, null, 0);
                return new ResponseEntity<>("We could not process your refund at this time. Your current booking remains active. Our system will retry automatically. If the problem persists, please contact support.",HttpStatus.INTERNAL_SERVER_ERROR);
            } else if (status == HttpStatus.TOO_MANY_REQUESTS) {;
                long retryAfterSeconds = defautRetryAfter;
                Optional<String> retryAfterOpt = refundResponse.headers().firstValue("Retry-After");
                if (retryAfterOpt.isPresent())
                    retryAfterSeconds = Long.parseLong(retryAfterOpt.get());
                this.refundService.processRefundAsync(booking, payment, retryAfterSeconds, 0);
                return new ResponseEntity<>("We could not process your refund at this time. Your current booking remains active. Our system will retry automatically. If the problem persists, please contact support.",HttpStatus.INTERNAL_SERVER_ERROR);
            } else {
                payment.setRefundStatus(RefundStatus.FAILED);
                paymentRepository.save(payment);
                try{
                    bookingEventPublisher.publishBookingEvent(booking, MQEventType.BOOKING_REFUND_FAILED);
                }
                catch(Exception e){
                    log.error("Unsucessfully publish modify failed event to RabbitMQ");
                }
                ObjectMapper mapper = new ObjectMapper();
                StripeErrorResponseDto errorResponse = mapper.readValue(refundResponse.body(), StripeErrorResponseDto.class);
                return new ResponseEntity<>(errorResponse.getError().getMessage(),HttpStatus.BAD_REQUEST);
            }
        }
        catch(Exception e){
            return new ResponseEntity<>("Unable to create a refund request. Please try again later!", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
