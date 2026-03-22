package com.example.springboot.service;

import com.example.springboot.model.NotificationEmail;
import com.example.springboot.model.NotificationStatus;
import com.example.springboot.repository.NotificationRepository;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;
    @Value("${spring.mail.username}")
    private String sender;
    @Value("${email.max-retries}")
    private int maxRetries;
    private final NotificationRepository notificationRepository;

    public EmailService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void sendEmail(NotificationEmail notificationEmail) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(notificationEmail.getRecipient());
        helper.setSubject(notificationEmail.getSubject());
        helper.setText(notificationEmail.getBody(), true); // HTML
        helper.setFrom(sender);
        try{
            mailSender.send(message);
            notificationEmail.setStatus(NotificationStatus.SENT);
            notificationRepository.save(notificationEmail);
        }
        catch (Exception e){
            notificationEmail.setStatus(NotificationStatus.RETRYING);
            retryEmail(notificationEmail);
        }

    }
    @Transactional // every 1 minute
    public void retryEmail(NotificationEmail notificationEmail) throws Exception {
        if(notificationEmail.getRetryAttempts() == null){
            notificationEmail.setRetryAttempts(0);
        }
        if(notificationEmail.getRetryAttempts() <= maxRetries && notificationEmail.getStatus() == NotificationStatus.RETRYING){
            notificationEmail.setRetryAttempts(notificationEmail.getRetryAttempts() + 1);
            notificationEmail.setLastRetryAt(java.time.LocalDateTime.now());
            sendEmail(notificationEmail);
            notificationRepository.save(notificationEmail);
        }
        else{
            notificationEmail.setStatus(NotificationStatus.FAILED);
            notificationRepository.save(notificationEmail);
        }
    }

    @Scheduled(fixedDelayString = "${email.retry-delay}") // every 5 minute
    public void retryFailedEmails() throws Exception {
        var failedEmails = notificationRepository.findByStatus(NotificationStatus.RETRYING);
        for (var email : failedEmails) {
            try {
                retryEmail(email);
            } catch (Exception e) {
                log.error("Failed to retry email to " + email.getRecipient() + " due to: ", e.getMessage());
            }
        }
    }

}