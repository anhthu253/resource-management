package com.example.springboot.mapper;

import com.example.springboot.dto.BookingDto;
import com.example.springboot.dto.BookingRequestDto;
import com.example.springboot.dto.ResourceDto;
import com.example.springboot.model.Booking;
import com.example.springboot.model.User;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.service.ResourceService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
@Component
public class BookingMapper {
    private final UserRepository userRepository;
    private final ResourceService resourceService;
    public BookingMapper(UserRepository userRepository, ResourceService resourceService) {

        this.userRepository = userRepository;
        this.resourceService = resourceService;
    }

    public Booking mapBookingRequestToBookingEntity(BookingRequestDto bookingRequest){

            Booking booking = new Booking();
            booking.setResourceIds(bookingRequest.getResourceIds());
            booking.setStartedAt(bookingRequest.getStartedAt());
            booking.setEndedAt(bookingRequest.getEndedAt());
            booking.setTotalPrice(bookingRequest.getTotalPrice());
            User user = this.userRepository.findById(bookingRequest.getUserId()).orElseThrow(() -> new RuntimeException("User not found"));
            booking.setUser(user);
            return booking;
    }

    public BookingDto mapBookingToBookingDto(Booking booking){
        List<ResourceDto> allResource = resourceService.getAllResources().block();
        var bookingDto = new BookingDto();
        bookingDto.setStatus(booking.getStatus());
        bookingDto.setStartedAt(booking.getStartedAt());
        bookingDto.setEndedAt(booking.getEndedAt());
        bookingDto.setTotalPrice(booking.getTotalPrice());
        List<String> resources = allResource.stream().filter(r -> booking.getResourceIds().contains(r.getResourceId())).map(r -> r.getResourceName()).collect(Collectors.toList());
        bookingDto.setResources(resources);
        return bookingDto;
    }
}
