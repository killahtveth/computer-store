package ru.ulstu.report.dto;

import java.math.BigDecimal;

public record ComputerReportItem(
        Long id,
        String model,
        String statusCode,
        String statusName,
        BigDecimal currentPrice
) {}