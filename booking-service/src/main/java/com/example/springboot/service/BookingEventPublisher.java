package com.example.springboot.service;

import com.example.springboot.model.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookingEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ResourceService resourceService;

    public BookingEventPublisher(RabbitTemplate rabbitTemplate, ResourceService resourceService) {
        this.rabbitTemplate = rabbitTemplate;
        this.resourceService = resourceService;
    }

    public void publishBookingEvent(Booking booking, MQEventType bookingEventType){
        User user = booking.getUser();
        List<String> resourceNames = resourceService.getAllResources().block().stream()
                .filter(r -> booking.getResourceIds().contains(r.getResourceId()))
                .map(r -> r.getResourceName())
                .collect(Collectors.toList());
        BookingEvent event = BookingEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .bookingId(booking.getBookingNumber())
                .bookingNumber(booking.getBookingNumber())
                .startTime(booking.getStartedAt())
                .endTime(booking.getEndedAt())
                .resourceNames(resourceNames)
                .amount(booking.getTotalPrice())
                .bookingEventType(bookingEventType)
                .userEmail(user.getEmail())
                .userFullName(user.getFirstName() + " " + user.getLastName()).build();
        rabbitTemplate.convertAndSend(
                RabbitConfig.BOOKING_EXCHANGE,
                event.getBookingEventType().toString(),
                event
        );
    }
}