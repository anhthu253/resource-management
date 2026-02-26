package com.example.springboot.mapper;

import com.example.springboot.dto.BookingDto;
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
    public Booking mapBookingDtoToBooking(BookingDto bookingDto){
        List<ResourceDto> allResource = resourceService.getAllResources().block();
        var booking = new Booking();
        booking.setBookingStatus(bookingDto.getBookingStatus());
        booking.setModificationStatus(bookingDto.getModificationStatus());
        booking.setStartedAt(bookingDto.getStartedAt());
        booking.setEndedAt(bookingDto.getEndedAt());
        booking.setTotalPrice(bookingDto.getTotalPrice());
        User user = userRepository.findById(bookingDto.getUserId()).orElseThrow(()-> new IllegalArgumentException("User not found"));
        booking.setUser(user);
        List<Long> resourceIds = bookingDto.getResources().stream().map(resource -> resource.getResourceId()).collect(Collectors.toList());
        booking.setResourceIds(resourceIds);
        return booking;
    }
    public BookingDto mapBookingToBookingDto(Booking booking){
        List<ResourceDto> allResource = resourceService.getAllResources().block();
        var bookingDto = new BookingDto();
        bookingDto.setBookingId(booking.getBookingId());
        bookingDto.setBookingNumber(booking.getBookingNumber());
        bookingDto.setPaymentId(booking.getPayment().getPaymentId());
        bookingDto.setBookingStatus(booking.getBookingStatus());
        bookingDto.setModificationStatus(booking.getModificationStatus());
        bookingDto.setStartedAt(booking.getStartedAt());
        bookingDto.setEndedAt(booking.getEndedAt());
        bookingDto.setTotalPrice(booking.getTotalPrice());
        List<ResourceDto> resources = allResource.stream().filter(r -> booking.getResourceIds().contains(r.getResourceId())).collect(Collectors.toList());
        bookingDto.setResources(resources);
        return bookingDto;
    }

    public List<BookingDto> mapBookingListToBookingDtoList(List<Booking> bookings){
        return bookings.stream().map(booking -> mapBookingToBookingDto(booking)).collect(Collectors.toList());
    }
}
