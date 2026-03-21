package com.example.springboot.service;

import com.example.springboot.model.BookingEvent;
import com.example.springboot.model.NotificationEmail;
import com.example.springboot.model.NotificationStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
@Slf4j
@Service
public class NotificationService {
    @Autowired
    private EmailService emailService;
    @Autowired
    private TemplateEngine templateEngine;
    @RabbitListener(queues = "booking.created.queue")
    public void handleBookingCreated(BookingEvent event) {
        String to = event.getUserEmail();
        Context context = createBookingEmailContext(event);
        String htmlContent = templateEngine.process("booking-confirm", context);
        NotificationEmail notificationEmail = createNotificationEmail(to, "Booking Confirmation", htmlContent);
        try{
            emailService.sendEmail(notificationEmail);
        }
        catch (Exception e){
            log.error("Cannot send confirmation email to " + event.getUserEmail() + " due to: ", e.getMessage());
        }

    }
    @RabbitListener(queues = "cancel.succeeded.queue")
    public void handleBookingCanceled(BookingEvent event){
        String to = event.getUserEmail();
        Context context = createBookingEmailContext(event);
        String htmlContent = templateEngine.process("booking-canceled", context);
        NotificationEmail notificationEmail = createNotificationEmail(to, "Booking Cancellation Confirmation", htmlContent);
        try{
            emailService.sendEmail(notificationEmail);
        }
        catch (Exception e){
            log.error("Cannot send booking canceled confirmation email to " + event.getUserEmail() + " due to: ", e.getMessage());
        }
    }

    @RabbitListener(queues = "cancel.failed.queue")
    public void handleBookingCancelFailed(BookingEvent event){
        String to = event.getUserEmail();
        Context context = createBookingEmailContext(event);
        String htmlContent = templateEngine.process("booking-cancel-failed", context);
        NotificationEmail notificationEmail = createNotificationEmail(to, "Booking Cancellation Failed", htmlContent);
        try{
            emailService.sendEmail(notificationEmail);
        }
        catch (Exception e){
            log.error("Cannot send email of a failed booking cancellation to " + event.getUserEmail()+ " due to: ", e.getMessage());
        }
    }

    @RabbitListener(queues = "modify.succeeded.queue")
    public void handleBookingRefunded(BookingEvent event){
        String to = event.getUserEmail();
        Context context = createBookingEmailContext(event);
        String htmlContent = templateEngine.process("booking-refunded", context);
        NotificationEmail notificationEmail = createNotificationEmail(to, "Booking Refund Confirmation", htmlContent);
        try{
            emailService.sendEmail(notificationEmail);
        }
        catch (Exception e){
            log.error("Cannot send email of a refunded booking to " + event.getUserEmail() + " due to: ", e.getMessage());
        }
    }
    @RabbitListener(queues = "modify.failed.queue")
    public void handleBookingRefundFailed(BookingEvent event){
        String to = event.getUserEmail();
        Context context = createBookingEmailContext(event);

        String htmlContent = templateEngine.process("booking-refund-failed", context);
        NotificationEmail notificationEmail = createNotificationEmail(to, "Booking Refund Failed", htmlContent);
        try{
            emailService.sendEmail(notificationEmail);
        }
        catch (Exception e){
            log.error("Cannot send email of a failed refund to " + event.getUserEmail() + " due to: ", e.getMessage());
        }
    }
    private Context createBookingEmailContext(BookingEvent event){
        Context context = new Context();
        context.setVariable("customerName", event.getUserFullName());
        context.setVariable("bookingNumber",event.getBookingNumber());
        context.setVariable("resources", event.getResourceNames());
        context.setVariable("startDate",event.getStartTime());
        context.setVariable("endDate",event.getEndTime());
        context.setVariable("totalPrice",event.getAmount() + " EUR");
        return context;
    }

    private NotificationEmail createNotificationEmail(String to, String subject, String htmlContent){
        return NotificationEmail.builder()
                .recipient(to)
                .subject(subject)
                .body(htmlContent)
                .status(NotificationStatus.PENDING)
                .build();
    }
}
