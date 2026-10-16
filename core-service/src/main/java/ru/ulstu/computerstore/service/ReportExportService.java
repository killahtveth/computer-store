package ru.ulstu.computerstore.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ulstu.computerstore.dto.ComputerResponse;
import ru.ulstu.computerstore.dto.ReportResponse;
import ru.ulstu.computerstore.entity.OutboxEvent;
import ru.ulstu.computerstore.messaging.ReportExportMessage;
import ru.ulstu.computerstore.messaging.SagaMessage;
import ru.ulstu.computerstore.repository.OutboxEventRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportExportService {

    private final ComputerService computerService;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public String requestStatusExport() {
        ReportResponse report = computerService.getReport();

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("availableCount", report.availableCount());
        status.put("soldCount", report.soldCount());
        status.put("totalCount", report.availableCount() + report.soldCount());

        String requestId = UUID.randomUUID().toString();

        ReportExportMessage message = new ReportExportMessage(
                requestId,
                "STATUS",
                MDC.get("traceId"),
                LocalDateTime.now().toString(),
                status,
                null
        );

        saveOutbox("REPORT_EXPORT_STATUS", message);
        return requestId;
    }

    @Transactional
    public String requestComputersExport() {
        List<ComputerResponse> computers = computerService.getAllComputers();

        List<Map<String, Object>> data = computers.stream()
                .map(this::toMap)
                .toList();

        String requestId = UUID.randomUUID().toString();

        ReportExportMessage message = new ReportExportMessage(
                requestId,
                "COMPUTERS",
                MDC.get("traceId"),
                LocalDateTime.now().toString(),
                null,
                data
        );

        saveOutbox("REPORT_EXPORT_COMPUTERS", message);
        return requestId;
    }

    /**
     * Запускает сагу: собрать данные → сохранить в outbox → orchestrator
     * получит команду saga.start и начнёт цепочку.
     *
     *  если true — requestId будет начинаться с "fail-",
     *                              чтобы notification-service симулировал сбой
     *                              (для демонстрации компенсации)
     */
    @Transactional
    public String requestFullExport(boolean forceFailNotification) {
        ReportResponse report = computerService.getReport();

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("availableCount", report.availableCount());
        status.put("soldCount", report.soldCount());
        status.put("totalCount", report.availableCount() + report.soldCount());

        String requestId = (forceFailNotification ? "fail-" : "") + UUID.randomUUID();
        String sagaId = UUID.randomUUID().toString();

        SagaMessage message = new SagaMessage(
                sagaId,
                requestId,
                MDC.get("traceId"),
                "STATUS",
                status,
                null,
                null,
                null
        );

        saveOutboxSaga(message);
        return sagaId;
    }

    private Map<String, Object> toMap(ComputerResponse c) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", c.id());
        map.put("model", c.model());
        map.put("statusCode", c.statusCode());
        map.put("statusName", c.statusName());
        map.put("currentPrice", currentPrice(c));
        return map;
    }

    private BigDecimal currentPrice(ComputerResponse c) {
        if (c.priceHistory() == null || c.priceHistory().isEmpty()) {
            return null;
        }
        return c.priceHistory().stream()
                .filter(h -> h.validTo() == null)
                .findFirst()
                .map(ComputerResponse.PriceHistoryDto::price)
                .orElse(c.priceHistory().get(0).price());
    }

    private void saveOutbox(String eventType, ReportExportMessage message) {
        try {
            OutboxEvent event = new OutboxEvent();
            event.setEventId(message.requestId());
            event.setEventType(eventType);
            event.setPayload(objectMapper.writeValueAsString(message));
            event.setStatus("NEW");
            event.setCreatedAt(LocalDateTime.now());
            event.setRetryCount(0);
            outboxEventRepository.save(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Не удалось сериализовать outbox-сообщение", e);
        }
    }

    private void saveOutboxSaga(SagaMessage message) {
        try {
            OutboxEvent event = new OutboxEvent();
            event.setEventId(message.sagaId());
            event.setEventType("REPORT_EXPORT_FULL");
            event.setPayload(objectMapper.writeValueAsString(message));
            event.setStatus("NEW");
            event.setCreatedAt(LocalDateTime.now());
            event.setRetryCount(0);
            outboxEventRepository.save(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Не удалось сериализовать saga-сообщение", e);
        }
    }
}