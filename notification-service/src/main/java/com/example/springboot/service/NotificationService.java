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
            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            log.error("Cannot send confirmation email to " + event.getUserEmail() + " due to: ", cause);
        }

    }

    @RabbitListener(queues = "booking.failed.queue")
    public void handleBookingFailed(BookingEvent event) {
        String to = event.getUserEmail();
        Context context = createBookingEmailContext(event);
        String htmlContent = templateEngine.process("booking-failed", context);
        NotificationEmail notificationEmail = createNotificationEmail(to, "Booking Failed", htmlContent);
        try{
            emailService.sendEmail(notificationEmail);
        }
        catch (Exception e){
            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            log.error("Cannot send email of failed booking to " + event.getUserEmail() + " due to: ", cause);
        }
    }
    @RabbitListener(queues = "modify.succeeded.queue")
    public void handleBookingModified(BookingEvent event) {
        String to = event.getUserEmail();
        Context context = createBookingEmailContext(event);
        String htmlContent = templateEngine.process("booking-modified", context);
        NotificationEmail notificationEmail = createNotificationEmail(to, "Booking Modification Confirmation", htmlContent);
        try{
            emailService.sendEmail(notificationEmail);
        }
        catch (Exception e){
            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            log.error("Cannot send confirmation of booking modification to " + event.getUserEmail() + " due to: ", cause);
        }
    }

    @RabbitListener(queues = "modify.failed.queue")
    public void handleBookingModifyFailed(BookingEvent event) {
        String to = event.getUserEmail();
        Context context = createBookingEmailContext(event);
        String htmlContent = templateEngine.process("booking-modify-failed", context);
        NotificationEmail notificationEmail = createNotificationEmail(to, "Booking Modification Failed", htmlContent);
        try{
            emailService.sendEmail(notificationEmail);
        }
        catch (Exception e){
            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            log.error("Cannot send email of failed modified booking to " + event.getUserEmail() + " due to: ", cause);
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
            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            log.error("Cannot send booking canceled confirmation email to " + event.getUserEmail() + " due to: ", cause);
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
            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            log.error("Cannot send email of a failed booking cancellation to " + event.getUserEmail()+ " due to: ", cause);
        }
    }

    @RabbitListener(queues = "refund.succeeded.queue")
    public void handleBookingRefunded(BookingEvent event){
        String to = event.getUserEmail();
        Context context = createBookingEmailContext(event);
        String htmlContent = templateEngine.process("booking-refunded", context);
        NotificationEmail notificationEmail = createNotificationEmail(to, "Booking Refund Confirmation", htmlContent);
        try{
            emailService.sendEmail(notificationEmail);
        }
        catch (Exception e){
            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            log.error("Cannot send email of a refunded booking to " + event.getUserEmail() + " due to: ", cause);
        }
    }
    @RabbitListener(queues = "refund.failed.queue")
    public void handleBookingRefundFailed(BookingEvent event){
        String to = event.getUserEmail();
        Context context = createBookingEmailContext(event);

        String htmlContent = templateEngine.process("booking-refund-failed", context);
        NotificationEmail notificationEmail = createNotificationEmail(to, "Booking Refund Failed", htmlContent);
        try{
            emailService.sendEmail(notificationEmail);
        }
        catch (Exception e){
            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            log.error("Cannot send email of a failed refund to " + event.getUserEmail() + " due to: ", cause);
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
        context.setVariable("previousBookingNumber", event.getPreviousBookingNumber());
        context.setVariable("previousStartDate", event.getPreviousStartTime());
        context.setVariable("previousEndDate", event.getPreviousEndTime());
        context.setVariable("previousResources", event.getPreviousResourceNames());
        context.setVariable("previousTotalPrice", event.getPreviousAmount() != null ? event.getPreviousAmount() + " EUR" : null);
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
