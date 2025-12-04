package com.example.springboot.model;


import jakarta.persistence.*;

@Entity
@Table(name="notification_template")
public class NotificationTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationTemplateId;
    private String name;
    private String subject;
    private String body;
}
