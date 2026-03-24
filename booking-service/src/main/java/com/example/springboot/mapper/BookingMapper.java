package com.example.springboot.mapper;

import com.example.springboot.dto.BookingDto;
import com.example.springboot.dto.ResourceDto;
import com.example.springboot.model.Booking;
import com.example.springboot.model.User;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.service.ResourceService;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.time.ZoneId;
@Component
public class BookingMapper {
    private final UserRepository userRepository;
    private final ResourceService resourceService;
    public BookingMapper(UserRepository userRepository, ResourceService resourceService) {

        this.userRepository = userRepository;
        this.resourceService = resourceService;
    }
    public Booking mapBookingDtoToBooking(BookingDto bookingDto){
        if(bookingDto == null) return null;
        var booking = new Booking();
        booking.setBookingGroupId(bookingDto.getBookingGroupId());
        booking.setBookingStatus(bookingDto.getBookingStatus());
        booking.setModificationStatus(bookingDto.getModificationStatus());
        booking.setStartedAt(bookingDto.getStartedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        booking.setEndedAt(bookingDto.getEndedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        booking.setTotalPrice(bookingDto.getTotalPrice());
        User user = userRepository.findById(bookingDto.getUserId()).orElseThrow(()-> new IllegalArgumentException("User not found"));
        booking.setUser(user);
        List<Long> resourceIds = bookingDto.getResources().stream().map(resource -> resource.getResourceId()).collect(Collectors.toList());
        booking.setResourceIds(resourceIds);
        return booking;
    }
    public BookingDto mapBookingToBookingDto(Booking booking) {
        if(booking == null) return null;
        try {
            List<ResourceDto> allResource = resourceService.getAllResources();
            var bookingDto = new BookingDto();
            bookingDto.setBookingId(booking.getBookingId());
            bookingDto.setBookingGroupId(booking.getBookingGroupId());
            bookingDto.setBookingNumber(booking.getBookingNumber());
            bookingDto.setPaymentId(booking.getPayment().getPaymentId());
            bookingDto.setBookingStatus(booking.getBookingStatus());
            bookingDto.setModificationStatus(booking.getModificationStatus());
            bookingDto.setStartedAt(Date.from(booking.getStartedAt().atZone(ZoneId.systemDefault()).toInstant()));
            bookingDto.setEndedAt(Date.from(booking.getEndedAt().atZone(ZoneId.systemDefault()).toInstant()));
            bookingDto.setTotalPrice(booking.getTotalPrice());
            bookingDto.setCreatedAt(Date.from(booking.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant()));
            List<ResourceDto> resources = allResource.stream().filter(r -> booking.getResourceIds().contains(r.getResourceId())).collect(Collectors.toList());
            bookingDto.setResources(resources);
            return bookingDto;
        }
        catch (Exception ex){
            return null;
        }
    }
    public List<BookingDto> mapBookingListToBookingDtoList(List<Booking> bookings){
        return bookings.stream().map(booking -> mapBookingToBookingDto(booking)).collect(Collectors.toList());
    }
}
