package ru.ulstu.computerstore.dto;

import java.math.BigDecimal;
import java.util.List;

public record ComputerResponse(
        Long id,
        String model,
        String statusCode,
        String statusName,
        List<PriceHistoryDto> priceHistory
) {
    public record PriceHistoryDto(
            Long id,
            BigDecimal price,
            String validFrom,
            String validTo
    ) {}
}