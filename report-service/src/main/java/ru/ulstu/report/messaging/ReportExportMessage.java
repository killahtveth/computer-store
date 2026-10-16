package ru.ulstu.report.messaging;

import java.util.List;
import java.util.Map;

public record ReportExportMessage(
        String requestId,
        String reportType,
        String traceId,
        String createdAt,
        Map<String, Object> status,
        List<Map<String, Object>> computers
) {}