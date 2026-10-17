package ru.ulstu.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.ulstu.notification.entity.Notification;
import ru.ulstu.notification.repository.NotificationRepository;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSenderService {

    private final NotificationRepository notificationRepository;

    public Notification send(String sagaId, String requestId, String message) {
        if (requestId != null && requestId.startsWith("fail-")) {
            throw new IllegalStateException("Симуляция сбоя отправки уведомления: " + requestId);
        }

        Notification n = new Notification();
        n.setSagaId(sagaId);
        n.setRequestId(requestId);
        n.setMessage(message);
        n.setStatus("SENT");
        n.setCreatedAt(LocalDateTime.now());

        Notification saved = notificationRepository.save(n);
        log.info("Уведомление отправлено: sagaId={}, requestId={}", sagaId, requestId);
        return saved;
    }
}