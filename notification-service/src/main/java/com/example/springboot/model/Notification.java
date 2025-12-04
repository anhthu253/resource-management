package com.example.springboot.model;

import jakarta.persistence.*;

@Entity
@Table(name="notification")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;
    private Long userId;
    private Long templateId;
    private NotificationType type;
    private NotificationStatus status;
}
