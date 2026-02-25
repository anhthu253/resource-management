package com.example.springboot.service;

import com.example.springboot.dto.RefundStatusUpdateDto;
import com.example.springboot.dto.StripeErrorResponseDto;
import com.example.springboot.model.*;
import com.example.springboot.repository.BookingRepository;
import com.example.springboot.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RefundService {
    @Value("${refund.retry.after}")
    private int defautRetryAfter;
    @Value("${max.refund.retries}")
    private int maxRetries;
    private final StripeService stripeService;
    private final Map<Long, Sinks.Many<RefundStatusUpdateDto>> sinks ;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    @Autowired
    @Lazy
    private RefundService self; // self proxy for @Async recursion

    public RefundService(StripeService stripeService, BookingRepository bookingRepository, PaymentRepository paymentRepository) {
        this.stripeService = stripeService;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.sinks = new ConcurrentHashMap<>();
    }

    @Async
    public void processRefundAsync(Booking booking, Payment payment, Long retryAfterSeconds, int attempt) {
        try{
            if(retryAfterSeconds !=  null) //for error code HTTPStatus.TOO_MANY_REQUESTS
                Thread.sleep(retryAfterSeconds*1000L);
            else
                Thread.sleep(attempt*1000L); //exponential backoff, for error code 5xx
        }
        catch(InterruptedException e){
            Thread.currentThread().interrupt();
        }

        long bookingId = booking.getBookingId();
        try {
            HttpResponse<String> refundResponse = stripeService.postPaymentRefund(bookingId, payment.getChargeId());
            HttpStatus status = HttpStatus.valueOf(refundResponse.statusCode());
            if(status.is2xxSuccessful()){
                booking.setModificationStatus(ModificationStatus.MODIFIED);
                bookingRepository.save(booking);
                payment.setPaymentStatus(PaymentStatus.REFUNDED);
                paymentRepository.save(payment);
                var message = "Refund request is successfully created";
                var update = new RefundStatusUpdateDto(bookingId, payment.getPaymentStatus(), message);
                notifyUpdate(update);
                complete(bookingId);
            }
            else if(status==HttpStatus.TOO_MANY_REQUESTS||status.is5xxServerError()){
                if(attempt == maxRetries){
                    booking.setModificationStatus(ModificationStatus.MODIFY_FAILED);
                    bookingRepository.save(booking);
                    payment.setPaymentStatus(PaymentStatus.REFUND_FAILED);
                    paymentRepository.save(payment);
                    var message = "Max retries reached for refund request";
                    var update = new RefundStatusUpdateDto(bookingId, payment.getPaymentStatus(), message);
                    notifyUpdate(update);
                    complete(bookingId);
                }

                else {
                    self.processRefundAsync(booking, payment, retryAfterSeconds, attempt + 1);
                }
            }
            else { //4xx error
                booking.setModificationStatus(ModificationStatus.MODIFY_FAILED);
                bookingRepository.save(booking);
                payment.setPaymentStatus(PaymentStatus.REFUND_FAILED);
                paymentRepository.save(payment);
                ObjectMapper mapper = new ObjectMapper();
                StripeErrorResponseDto errorResponse = mapper.readValue(refundResponse.body(), StripeErrorResponseDto.class);
                var message = errorResponse.getError().getMessage();
                var update = new RefundStatusUpdateDto(bookingId, payment.getPaymentStatus(), message);
                notifyUpdate(update);
                complete(bookingId);
            }
        }
        catch(Exception e){
            var message = "Refund request is failed to proceeded. Please contact us!";
            var update = new RefundStatusUpdateDto(bookingId, payment.getPaymentStatus(), message   );
            notifyUpdate(update);
            complete(bookingId);
        }

    }

    public Flux<RefundStatusUpdateDto> getStatusStream(long bookingId) {
        return getOrCreateSink(bookingId).asFlux();
    }
    private void notifyUpdate(RefundStatusUpdateDto update) {
        Sinks.Many<RefundStatusUpdateDto> sink = getOrCreateSink(update.bookingId());
        if (sink != null) {
            sink.tryEmitNext(update);
        }
    }

    private Sinks.Many<RefundStatusUpdateDto> getOrCreateSink(long bookingId) {
        return sinks.computeIfAbsent(bookingId,
                id -> Sinks.many().replay().limit(10));
    }
    private void complete(long bookingId) {
        Sinks.Many<RefundStatusUpdateDto> sink = sinks.remove(bookingId);
        if (sink != null) {
            sink.tryEmitComplete();
        }
    }
}
