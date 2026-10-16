package ru.ulstu.saga.messaging;

import java.util.Map;

public record SagaMessage(
        String sagaId,
        String requestId,
        String traceId,
        String reportType,        // STATUS или COMPUTERS
        Map<String, Object> status,
        java.util.List<Map<String, Object>> computers,
        String objectName,        // имя файла в MinIO (заполняется после шага 1)
        String error              // причина провала (если есть)
) {}