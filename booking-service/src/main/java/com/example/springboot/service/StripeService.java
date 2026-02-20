package com.example.springboot.service;

import com.example.springboot.dto.PaymentIntentDto;
import com.example.springboot.model.Booking;
import com.example.springboot.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
@Service
public class StripeService {
    private String auth;
    private String stripeBaseUrl;
    private final BookingRepository bookingRepository;
    private final HttpClient httpClient;

    public StripeService(@Value("${stripe.test.secret}") String stripeSecretKey, @Value("${stripe.test.url}") String stripeBaseUrl, BookingRepository bookingRepository) {
        this.auth = Base64.getEncoder().encodeToString((stripeSecretKey + ":").getBytes());
        this.stripeBaseUrl = stripeBaseUrl;
        this.bookingRepository = bookingRepository;
        this.httpClient = HttpClient.newHttpClient();
    }

    public HttpResponse<String> postPaymentIntents(PaymentIntentDto paymentIntentDto) throws Exception {
        Booking booking = bookingRepository.findById((paymentIntentDto.getBookingId())).orElseThrow();
        BigDecimal amountInCents = booking.getTotalPrice().multiply(new BigDecimal("100"));
        long amount = amountInCents.longValueExact();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(stripeBaseUrl+"/payment_intents"))
                .header("Authorization", "Basic " + auth)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(
                                "amount="+amount+"&" +
                                "currency=EUR&" +
                                "payment_method="+paymentIntentDto.getPaymentMethodId()
                ))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
    public HttpResponse<String> postPaymentRefund(String chargeId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(stripeBaseUrl+"/refunds"))
                .header("Authorization", "Basic " + auth)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "charge="+chargeId
                ))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

}
