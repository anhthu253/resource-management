package com.example.springboot.service;

import com.example.springboot.dto.PaymentIntentDto;
import com.example.springboot.dto.PaymentIntentResponseDto;
import com.example.springboot.dto.StatusUpdateDto;
import com.example.springboot.model.*;
import com.example.springboot.repository.BookingRepository;
import com.example.springboot.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.model.Charge;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.StripeObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;
@Slf4j
@Service
public class PaymentService {
    public final PaymentRepository paymentRepository;
    public final BookingRepository bookingRepository;
    public final BookingEventPublisher bookingEventPublisher;
    private final StripeService stripeService;
    private final StatusUpdateService statusUpdateService;

    public PaymentService(PaymentRepository paymentRepository, BookingRepository bookingRepository, BookingEventPublisher bookingEventPublisher, StripeService stripeService, StatusUpdateService statusUpdateService) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.bookingEventPublisher = bookingEventPublisher;
        this.stripeService = stripeService;
        this.statusUpdateService = statusUpdateService;
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
            case "refund.failed" -> {
                Refund refund = (Refund) stripeObject;
                String chargeId = refund.getCharge();
                Payment payment = paymentRepository.findByChargeId(chargeId)
                        .orElseThrow(() -> new IllegalArgumentException("Payment not found for Charge: " + chargeId));
                payment.setRefundStatus(RefundStatus.FAILED);
                paymentRepository.save(payment);
                Booking booking = bookingRepository.findByPayment(payment)
                        .orElseThrow(() -> new IllegalArgumentException("Booking not found for Payment: " + payment.getPaymentId()));
                var update = new StatusUpdateDto(booking.getBookingId(), RefundStatus.FAILED, "Failed to refund.");
                statusUpdateService.notifyUpdate(update);
                statusUpdateService.complete(booking.getBookingId());
                try{
                    bookingEventPublisher.publishBookingEvent(booking, MQEventType.BOOKING_REFUND_FAILED);
                }
                catch(Exception e){
                    log.error("Unsucessfully publish refund failed event to RabbitMQ");
                }

            }

            // Refund events
           case "charge.refunded" -> {
                Charge charge = (Charge) stripeObject;
                String chargeId = charge.getId();
                Long amount = charge.getAmount();

                Payment payment = paymentRepository.findByChargeId(chargeId)
                        .orElseThrow(() -> new IllegalArgumentException("Payment not found for Charge: " + chargeId));
               payment.setRefundStatus(RefundStatus.REFUNDED);
               payment.setRefundedAmount(amount);
               paymentRepository.save(payment);
                Booking booking = bookingRepository.findByPayment(payment)
                        .orElseThrow(() -> new IllegalArgumentException("Booking not found for Payment: " + payment.getPaymentId()));
               var update = new StatusUpdateDto(booking.getBookingId(), RefundStatus.REFUNDED, "Succeeded to refund.");
               statusUpdateService.notifyUpdate(update);
               statusUpdateService.complete(booking.getBookingId());
                try{
                    bookingEventPublisher.publishBookingEvent(booking, MQEventType.BOOKING_REFUNDED);
                }
                catch(Exception e){
                    log.error("Unsucessfully publish booking modified event to RabbitMQ");
                }
            }
        }
    }
    public ResponseEntity<?> createRefund(long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        Payment payment = paymentRepository.findByBookingId(bookingId).orElseThrow();
        Optional<Booking> unpaidBooking = bookingRepository.findByBookingGroupIdAndBookingStatus(booking.getBookingGroupId(), BookingStatus.PENDING_CONFIRMATION);
        if(unpaidBooking.isPresent()){
            payment.setRefundStatus(RefundStatus.NOT_ELIGIBLE);
            paymentRepository.save(payment);
            return ResponseEntity.ok().body(Map.of("status",RefundStatus.NOT_ELIGIBLE, "message", String.format("You have a pending booking (No. %s). Please complete the payment in the Pending Bookings tab before requesting a refund. If it has expired, create a new booking from this one using the Edit button (do not use the New Booking tab, as it will not be linked). The refund can only be processed after the new booking has been paid.", unpaidBooking.get().getBookingNumber())));
        }
        try {
            payment.setRefundStatus(RefundStatus.PENDING);
            paymentRepository.save(payment);
            var update = new StatusUpdateDto(bookingId, RefundStatus.PENDING, "Your refund request is being processed");
            statusUpdateService.notifyUpdate(update);
            HttpResponse<String> refundResponse = stripeService.postPaymentRefund(bookingId, payment.getChargeId());
            HttpStatus status = HttpStatus.valueOf(refundResponse.statusCode());
            if (status.is2xxSuccessful()) {
                return ResponseEntity.ok().body(Map.of("status",RefundStatus.PENDING, "message","Refund request is successfully created."));
            } else if (status.is4xxClientError()){
                payment.setRefundStatus(RefundStatus.FAILED);
                paymentRepository.save(payment);
                update = new StatusUpdateDto(bookingId, RefundStatus.FAILED, "We couldn’t create your refund request. Please try again.");
                statusUpdateService.notifyUpdate(update);
                statusUpdateService.complete(bookingId);
                try{
                    bookingEventPublisher.publishBookingEvent(booking, MQEventType.BOOKING_REFUND_FAILED);
                }
                catch(Exception e){
                    log.error("Unsucessfully publish modify failed event to RabbitMQ");
                }
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status",RefundStatus.FAILED, "message","Failed to create refund request."));
            }
            else{
                payment.setRefundStatus(RefundStatus.FAILED);
                paymentRepository.save(payment);
                update = new StatusUpdateDto(bookingId, RefundStatus.FAILED, "We couldn’t create your refund request. Please try again.");
                statusUpdateService.notifyUpdate(update);
                try{
                    bookingEventPublisher.publishBookingEvent(booking, MQEventType.BOOKING_REFUND_FAILED);
                }
                catch(Exception e){
                    log.error("Unsucessfully publish modify failed event to RabbitMQ");
                }
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", RefundStatus.FAILED, "message","Failed to create refund request."));
            }
        }
        catch(Exception e){
            var update = new StatusUpdateDto(bookingId, RefundStatus.FAILED, "We couldn’t create your refund request. Please try again.");
            statusUpdateService.notifyUpdate(update);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status",RefundStatus.FAILED, "message","Failed to create refund request."));
        }
    }
}
