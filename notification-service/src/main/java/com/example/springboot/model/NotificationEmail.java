package com.example.springboot.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@Entity
@Table(name = "notification_email")
public class NotificationEmail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Recipient of the email
    private String recipient;
    private String subject;
    private String body;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status;
    private Integer retryAttempts = 0;
    private LocalDateTime lastRetryAt;
}
