package ru.ulstu.computerstore.dto;

import java.math.BigDecimal;

public record ComputerRequest(
        String model,
        BigDecimal price
) {}