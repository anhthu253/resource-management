package com.example.springboot;

import com.example.springboot.model.NotificationEmail;
import com.example.springboot.model.NotificationStatus;
import com.example.springboot.repository.NotificationRepository;
import com.example.springboot.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;


import static org.mockito.Mockito.*;
import org.springframework.boot.test.mock.mockito.MockBean;
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class EmailServiceRetryTest {

    @MockBean
    private JavaMailSender mailSender;
    @MockBean
    private NotificationRepository notificationRepository;

    @Autowired
    private EmailService emailService;

    @Test
    public void testEmailRetryLogic() throws Exception {
        // 1. Create a mock email event (e.g., from RabbitMQ)
        NotificationEmail email = NotificationEmail.builder()
                .recipient("user@example.com")
                .subject("Booking Confirmation")
                .body("<h1>Your booking is confirmed!</h1>")
                .retryAttempts(0)
                .status(NotificationStatus.PENDING)
                .build();
        MimeMessage mimeMessage = mock(MimeMessage.class);

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        // 2. First attempt: simulate failure
        doThrow(new RuntimeException("fail"))
                .doNothing()
                .when(mailSender)
                .send(any(MimeMessage.class));

        emailService.sendEmail(email);

        // 5. Verify retry count and status
        assert email.getRetryAttempts() == 1;

        assert email.getStatus() == NotificationStatus.SENT;
        verify(mailSender, times(2)).send(any(MimeMessage.class));
    }
}

