package com.example.springboot.repository;

import com.example.springboot.model.NotificationEmail;
import com.example.springboot.model.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEmail, Long> {
    List<NotificationEmail> findByStatus(NotificationStatus status);
}
