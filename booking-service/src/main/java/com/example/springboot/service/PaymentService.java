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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.http.HttpResponse;
import java.util.Optional;

@Service
public class PaymentService {
    public final PaymentRepository paymentRepository;
    public final BookingRepository bookingRepository;
    private final StripeService stripeService;
    private final RefundService refundService;
    @Value("${refund.retry.after}")
    private long defautRetryAfter;

    public PaymentService(PaymentRepository paymentRepository, BookingRepository bookingRepository, StripeService stripeService, RefundService refundService) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.stripeService = stripeService;
        this.refundService = refundService;
    }

    public Payment createPayment(Booking booking) {
        var payment = new Payment();
        payment.setBooking(booking);
        payment.setPaymentStatus(PaymentStatus.AWAITING_CUSTOMER_ACTION);
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
                payment.setPaymentStatus(PaymentStatus.SUCCEEDED);
                booking.setBookingStatus(BookingStatus.CONFIRMED);
                status = HttpStatus.OK;
            }

            case "payment_intent.payment_failed" -> {
                PaymentIntent paymentIntent = (PaymentIntent) stripeObject;
                String paymentIntentId = paymentIntent.getId();

                payment = paymentRepository.findByPaymentIntentId(paymentIntentId)
                        .orElseThrow(() -> new IllegalArgumentException("Payment not found for PaymentIntent: " + paymentIntentId));
                booking = bookingRepository.findByPayment(payment)
                        .orElseThrow(() -> new IllegalArgumentException("Booking not found for Payment: " + payment.getPaymentId()));

                payment.setPaymentStatus(PaymentStatus.FAILED);
                status = HttpStatus.INTERNAL_SERVER_ERROR;
            }

            // Refund events
            case "charge.refunded" -> {
                Charge charge = (Charge) stripeObject;
                String chargeId = charge.getId();
                Long amount = charge.getAmount();

                payment = paymentRepository.findByChargeId(chargeId)
                        .orElseThrow(() -> new IllegalArgumentException("Payment not found for Charge: " + chargeId));
                booking = bookingRepository.findByPayment(payment)
                        .orElseThrow(() -> new IllegalArgumentException("Booking not found for Payment: " + payment.getPaymentId()));

                payment.setPaymentStatus(PaymentStatus.REFUNDED);
                payment.setRefundedAmount(amount);
                if(booking.getBookingStatus() == BookingStatus.CANCEL_PENDING)
                    booking.setBookingStatus(BookingStatus.CANCELED); // reverse booking to active
                else if(booking.getModificationStatus() == ModificationStatus.MODIFY_PENDING)
                    booking.setModificationStatus(ModificationStatus.MODIFIED);
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

                payment.setPaymentStatus(PaymentStatus.REFUND_FAILED);
                if(booking.getBookingStatus() == BookingStatus.CANCEL_PENDING)
                    booking.setBookingStatus(BookingStatus.CONFIRMED); // reverse booking to active
                else if(booking.getModificationStatus() == ModificationStatus.MODIFY_PENDING)
                    booking.setModificationStatus(ModificationStatus.MODIFY_FAILED);
                status = HttpStatus.INTERNAL_SERVER_ERROR;
            }
            default -> {
                payment=null;
                booking=null;
            }
        }
        // Save updates
        if(payment != null) paymentRepository.save(payment);
        if (booking != null) bookingRepository.save(booking);
        return status;
    }
    public ResponseEntity<?> createRefund(long bookingId) throws Exception {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        Payment payment = paymentRepository.findByBookingId(bookingId).orElseThrow();
        try {
            HttpResponse<String> refundResponse = stripeService.postPaymentRefund(bookingId, payment.getChargeId());
            HttpStatus status = HttpStatus.valueOf(refundResponse.statusCode());
            if (status.is2xxSuccessful()) {
                payment.setPaymentStatus(PaymentStatus.REFUND_PENDING);
                paymentRepository.save(payment);
                return new ResponseEntity<>("Refund request is successfully created.",HttpStatus.OK);
            } else if (status.is5xxServerError()) {
                this.refundService.processRefundAsync(booking, payment, null, 0);
                return new ResponseEntity<>("We could not process your refund at this time. Your current booking remains active. Our system will retry automatically. If the problem persists, please contact support.",HttpStatus.INTERNAL_SERVER_ERROR);
            } else if (status == HttpStatus.TOO_MANY_REQUESTS) {
                long retryAfterSeconds = defautRetryAfter;
                Optional<String> retryAfterOpt = refundResponse.headers().firstValue("Retry-After");
                if (retryAfterOpt.isPresent())
                    retryAfterSeconds = Long.parseLong(retryAfterOpt.get());
                this.refundService.processRefundAsync(booking, payment, retryAfterSeconds, 0);
                return new ResponseEntity<>("We could not process your refund at this time. Your current booking remains active. Our system will retry automatically. If the problem persists, please contact support.",HttpStatus.INTERNAL_SERVER_ERROR);
            } else {
                booking.setModificationStatus(ModificationStatus.MODIFY_FAILED);
                bookingRepository.save(booking);
                payment.setPaymentStatus(PaymentStatus.REFUND_FAILED);
                paymentRepository.save(payment);
                ObjectMapper mapper = new ObjectMapper();
                StripeErrorResponseDto errorResponse = mapper.readValue(refundResponse.body(), StripeErrorResponseDto.class);
                return new ResponseEntity<>(errorResponse.getError().getMessage(),HttpStatus.BAD_REQUEST);
            }
        }
        catch(Exception e){
            return new ResponseEntity<>("Refund request is failed to proceeded. Please contact us!", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
