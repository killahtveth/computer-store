package ru.ulstu.report.dto;

public record StatusReport(
        long availableCount,
        long soldCount,
        long totalCount
) {}