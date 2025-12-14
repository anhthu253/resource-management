package com.example.springboot.service;

import com.example.springboot.dto.BookingPeriodDto;
import com.example.springboot.dto.BookingResponseDto;
import com.example.springboot.dto.ResourceDto;
import com.example.springboot.mapper.BookingRequestMapper;

import com.example.springboot.model.*;
import com.example.springboot.repository.BookingRepository;
import com.example.springboot.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingService {
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final BookingRequestMapper bookingRequestMapper;
    private WebClient resourceWebClient;
    public BookingService(BookingRepository bookingRepository,
                          PaymentRepository paymentRepository,
                          BookingRequestMapper bookingRequestMapper,
                          @Qualifier("resourceWebClient") WebClient resourceWebClient) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.bookingRequestMapper = bookingRequestMapper;
        this.resourceWebClient = resourceWebClient;
    }

    public BookingResponseDto createBooking (Booking bookingRequest){
        bookingRequest.setStatus(BookingStatus.REQUESTED);
        Booking booking = this.bookingRepository.save(bookingRequest);
        Payment payment = createPayment(booking);
        return new BookingResponseDto(booking.getBookingId(), payment.getPaymentId(), PaymentStatus.PENDING);
    }

    public Payment createPayment(Booking booking){
        var payment = new Payment();
        payment.setBooking(booking);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(booking.getTotalPrice());
        payment.setCurrency(Currency.EUR);
        return this.paymentRepository.save(payment);
    }
    public List<ResourceDto> getAvailableResources(BookingPeriodDto bookingPeriodDto) {
        List<ResourceDto> allResource = getAllResources().block();
        List<Long> bookedResourceIds = this.bookingRepository.getBookings(bookingPeriodDto.startedAt(), bookingPeriodDto.endedAt())
                .stream().flatMap(booking -> booking.getResourceIds().stream())
                .distinct()
                .collect(Collectors.toList());
        if(bookedResourceIds.size() == 0) return allResource;
        return allResource.stream()
                .filter(resource -> !bookedResourceIds.contains(resource.getResourceId()))
                .collect(Collectors.toList());
    }
    private Mono<List<ResourceDto>> getAllResources(){
        return resourceWebClient.get().uri("/resource/all")
                .retrieve().bodyToMono(new ParameterizedTypeReference<List<ResourceDto>>() {});
    }


}
