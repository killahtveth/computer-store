package ru.ulstu.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.ulstu.notification.entity.Notification;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRequestId(String requestId);
    boolean existsByRequestIdAndStatus(String requestId, String status);
}