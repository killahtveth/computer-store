package ru.ulstu.saga.messaging;

import java.util.List;
import java.util.Map;

public record SagaMessage(
        String sagaId,
        String requestId,
        String traceId,
        String reportType,
        Map<String, Object> status,
        List<Map<String, Object>> computers,
        String objectName,
        String error
) {}