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

    public void publishBookingEvent(Booking currentBooking, Booking previousBooking, MQEventType bookingEventType) throws Exception {
        User user = currentBooking.getUser();
        List<String> resourceNames = resourceService.getAllResources().stream()
                .filter(r -> currentBooking.getResourceIds().contains(r.getResourceId()))
                .map(r -> r.getResourceName())
                .collect(Collectors.toList());

        BookingEvent event = BookingEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .bookingId(currentBooking.getBookingNumber())
                .bookingNumber(currentBooking.getBookingNumber())
                .startTime(currentBooking.getStartedAt())
                .endTime(currentBooking.getEndedAt())
                .resourceNames(resourceNames)
                .amount(currentBooking.getTotalPrice())
                .bookingEventType(bookingEventType)
                .userEmail(user.getEmail())
                .userFullName(user.getFirstName() + " " + user.getLastName()).build();

        if(previousBooking != null){
            List<String> previousResourceNames = resourceService.getAllResources().stream()
                    .filter(r -> previousBooking.getResourceIds().contains(r.getResourceId()))
                    .map(r -> r.getResourceName())
                    .collect(Collectors.toList());
            event.setPreviousBookingNumber(previousBooking.getBookingNumber());
            event.setPreviousStartTime(previousBooking.getStartedAt());
            event.setPreviousEndTime(previousBooking.getEndedAt());
            event.setPreviousResourceNames(previousResourceNames);
            event.setPreviousAmount(previousBooking.getTotalPrice());
        }

        rabbitTemplate.convertAndSend(
                RabbitConfig.BOOKING_EXCHANGE,
                event.getBookingEventType().toString(),
                event
        );
    }
}