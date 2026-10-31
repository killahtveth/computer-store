package ru.ulstu.sort;

import java.util.List;

public record SortResult<T>(
        List<T> sorted,
        long durationMs,
        int threadsUsed,
        int chunkCount
) {}