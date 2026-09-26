package ru.ulstu.computerstore.dto;

public record ReportResponse(
        long availableCount,
        long soldCount
) {}