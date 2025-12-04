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
    private String stripeSecretKey;
    private String auth;
    private String stripeBaseUrl;
    private final BookingRepository bookingRepository;

    public StripeService(@Value("${stripe.test.secret}") String stripeSecretKey, @Value("${stripe.test.url}") String stripeBaseUrl, BookingRepository bookingRepository) {
        this.stripeSecretKey = stripeSecretKey;
        this.auth = Base64.getEncoder().encodeToString((stripeSecretKey + ":").getBytes());
        this.stripeBaseUrl = stripeBaseUrl;
        this.bookingRepository = bookingRepository;
    }

    public HttpResponse<String> postPaymentIntents(PaymentIntentDto paymentIntentDto) throws Exception {
        Booking booking = bookingRepository.findById((paymentIntentDto.getBookingId())).orElseThrow();
        BigDecimal amountInCents = booking.getTotalPrice().multiply(new BigDecimal("100"));
        long amount = amountInCents.longValueExact();
        HttpClient client = HttpClient.newHttpClient();

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

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
