package ru.ulstu.saga.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ulstu.saga.config.RabbitConfig;
import ru.ulstu.saga.entity.SagaInstance;
import ru.ulstu.saga.entity.SagaOutboxEvent;
import ru.ulstu.saga.messaging.SagaMessage;
import ru.ulstu.saga.repository.SagaInstanceRepository;
import ru.ulstu.saga.repository.SagaOutboxEventRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SagaOrchestratorService {

    private final SagaInstanceRepository sagaRepo;
    private final SagaOutboxEventRepository outboxRepo;
    private final ObjectMapper objectMapper;

    // ============ СТАРТ САГИ ============

    @Transactional
    public void startSaga(SagaMessage msg) {
        if (sagaRepo.findBySagaId(msg.sagaId()).isPresent()) {
            log.warn("Сага {} уже существует — пропускаем дубликат", msg.sagaId());
            return;
        }

        SagaInstance saga = new SagaInstance();
        saga.setSagaId(msg.sagaId());
        saga.setRequestId(msg.requestId());
        saga.setStatus("STEP_1_IN_PROGRESS");
        saga.setCurrentStep("REPORT_UPLOAD");
        saga.setTraceId(msg.traceId());
        saga.setPayload(toJson(msg));
        saga.setCreatedAt(LocalDateTime.now());
        saga.setUpdatedAt(LocalDateTime.now());
        sagaRepo.save(saga);

        log.info(">>> Сага {} создана, публикуем команду upload", msg.sagaId());

        saveOutbox(saga.getSagaId(), "REPORT_UPLOAD_COMMAND",
                RabbitConfig.REPORT_UPLOAD_COMMAND_KEY, msg);
    }

    // ============ РЕЗУЛЬТАТ ШАГА 1: UPLOAD ============

    @Transactional
    public void onReportUploaded(SagaMessage msg) {
        SagaInstance saga = getSaga(msg.sagaId());

        if (!"STEP_1_IN_PROGRESS".equals(saga.getStatus())) {
            log.warn("Сага {} в статусе {} — игнорируем report.uploaded",
                    saga.getSagaId(), saga.getStatus());
            return;
        }

        saga.setStatus("STEP_2_IN_PROGRESS");
        saga.setCurrentStep("NOTIFY");
        saga.setObjectName(msg.objectName());
        saga.setUpdatedAt(LocalDateTime.now());
        sagaRepo.save(saga);

        log.info("<<< Шаг 1 (upload) выполнен для саги {}, публикуем команду notify",
                saga.getSagaId());

        SagaMessage notifyMsg = new SagaMessage(
                saga.getSagaId(),
                saga.getRequestId(),
                saga.getTraceId(),
                msg.reportType(),
                null, null,
                msg.objectName(),
                null
        );

        saveOutbox(saga.getSagaId(), "NOTIFY_COMMAND",
                RabbitConfig.NOTIFY_COMMAND_KEY, notifyMsg);
    }

    @Transactional
    public void onReportUploadFailed(SagaMessage msg) {
        SagaInstance saga = getSaga(msg.sagaId());
        saga.setStatus("FAILED");
        saga.setError("Upload failed: " + msg.error());
        saga.setUpdatedAt(LocalDateTime.now());
        sagaRepo.save(saga);
        log.warn("!!! Сага {} FAILED на шаге 1: {}", saga.getSagaId(), msg.error());
        // Компенсация не нужна: первый шаг не оставил следов
    }

    // ============ РЕЗУЛЬТАТ ШАГА 2: NOTIFY ============

    @Transactional
    public void onNotificationSent(SagaMessage msg) {
        SagaInstance saga = getSaga(msg.sagaId());

        if (!"STEP_2_IN_PROGRESS".equals(saga.getStatus())) {
            log.warn("Сага {} в статусе {} — игнорируем notification.sent",
                    saga.getSagaId(), saga.getStatus());
            return;
        }

        saga.setStatus("COMPLETED");
        saga.setUpdatedAt(LocalDateTime.now());
        sagaRepo.save(saga);
        log.info("+++ Сага {} COMPLETED", saga.getSagaId());
    }

    @Transactional
    public void onNotificationFailed(SagaMessage msg) {
        SagaInstance saga = getSaga(msg.sagaId());

        if (!"STEP_2_IN_PROGRESS".equals(saga.getStatus())) {
            log.warn("Сага {} в статусе {} — игнорируем notification.failed",
                    saga.getSagaId(), saga.getStatus());
            return;
        }

        saga.setStatus("COMPENSATING");
        saga.setCurrentStep("REPORT_DELETE");
        saga.setError("Notify failed: " + msg.error());
        saga.setUpdatedAt(LocalDateTime.now());
        sagaRepo.save(saga);

        log.warn("~~~ Сага {} переходит к компенсации: удаляем файл {}",
                saga.getSagaId(), saga.getObjectName());

        SagaMessage deleteMsg = new SagaMessage(
                saga.getSagaId(),
                saga.getRequestId(),
                saga.getTraceId(),
                msg.reportType(),
                null, null,
                saga.getObjectName(),
                null
        );

        saveOutbox(saga.getSagaId(), "REPORT_DELETE_COMMAND",
                RabbitConfig.REPORT_DELETE_COMMAND_KEY, deleteMsg);
    }

    // ============ КОМПЕНСАЦИЯ ============

    @Transactional
    public void onReportDeleted(SagaMessage msg) {
        SagaInstance saga = getSaga(msg.sagaId());

        if (!"COMPENSATING".equals(saga.getStatus())) {
            log.warn("Сага {} в статусе {} — игнорируем report.deleted",
                    saga.getSagaId(), saga.getStatus());
            return;
        }

        saga.setStatus("COMPENSATED");
        saga.setUpdatedAt(LocalDateTime.now());
        sagaRepo.save(saga);
        log.info("### Сага {} COMPENSATED", saga.getSagaId());
    }

    @Transactional
    public void onReportDeleteFailed(SagaMessage msg) {
        SagaInstance saga = getSaga(msg.sagaId());
        saga.setStatus("FAILED");
        saga.setError("Компенсация не удалась: " + msg.error());
        saga.setUpdatedAt(LocalDateTime.now());
        sagaRepo.save(saga);
        log.error("### Сага {} FAILED на компенсации: {}", saga.getSagaId(), msg.error());
    }

    // ============ helpers ============

    private SagaInstance getSaga(String sagaId) {
        return sagaRepo.findBySagaId(sagaId)
                .orElseThrow(() -> new IllegalStateException("Сага не найдена: " + sagaId));
    }

    private void saveOutbox(String sagaId, String eventType, String routingKey, SagaMessage msg) {
    try {
        SagaOutboxEvent event = new SagaOutboxEvent();
        event.setEventId(UUID.randomUUID().toString());  // ← только UUID, 36 символов
        event.setEventType(eventType);
        event.setRoutingKey(routingKey);
        event.setPayload(objectMapper.writeValueAsString(msg));
        event.setStatus("NEW");
        event.setCreatedAt(LocalDateTime.now());
        event.setRetryCount(0);
        outboxRepo.save(event);
    } catch (JsonProcessingException e) {
        throw new RuntimeException("Не удалось сохранить saga-outbox", e);
    }
}

    private String toJson(SagaMessage msg) {
        try {
            return objectMapper.writeValueAsString(msg);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}