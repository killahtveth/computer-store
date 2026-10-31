package ru.ulstu.sort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ParallelMergeSort {

    /**
     * Параллельная сортировка слиянием.
     *
     * @param input      исходный список (не модифицируется)
     * @param comparator компаратор
     * @param threadCount желаемое число потоков
     */
    public static <T> SortResult<T> sort(List<T> input,
                                         Comparator<? super T> comparator,
                                         int threadCount) {
        long start = System.currentTimeMillis();

        if (input == null || input.isEmpty()) {
            return new SortResult<>(List.of(), 0, 0, 0);
        }

        int effectiveThreads = Math.max(1, Math.min(threadCount, input.size()));

        // Последовательный режим
        if (effectiveThreads == 1) {
            List<T> copy = new ArrayList<>(input);
            sequentialMergeSort(copy, comparator);
            return new SortResult<>(copy,
                    System.currentTimeMillis() - start,
                    1, 1);
        }

        ExecutorService executor = Executors.newFixedThreadPool(effectiveThreads);

        try {
            // 1. Разбиваем на чанки
            List<List<T>> chunks = splitIntoChunks(input, effectiveThreads);
            int initialChunks = chunks.size();

            // 2. Сортируем каждый чанк параллельно
            List<CompletableFuture<List<T>>> futures = chunks.stream()
                    .map(chunk -> CompletableFuture.supplyAsync(() -> {
                        List<T> copy = new ArrayList<>(chunk);
                        sequentialMergeSort(copy, comparator);
                        return copy;
                    }, executor))
                    .toList();

            List<List<T>> sortedChunks = new ArrayList<>(
                    futures.stream().map(CompletableFuture::join).toList());

            // 3. Сливаем попарно в параллель, пока не останется один
            while (sortedChunks.size() > 1) {
                List<CompletableFuture<List<T>>> mergeFutures = new ArrayList<>();
                for (int i = 0; i < sortedChunks.size(); i += 2) {
                    if (i + 1 < sortedChunks.size()) {
                        final List<T> left = sortedChunks.get(i);
                        final List<T> right = sortedChunks.get(i + 1);
                        mergeFutures.add(CompletableFuture.supplyAsync(
                                () -> merge(left, right, comparator), executor));
                    } else {
                        mergeFutures.add(CompletableFuture.completedFuture(sortedChunks.get(i)));
                    }
                }
                sortedChunks = new ArrayList<>(
                        mergeFutures.stream().map(CompletableFuture::join).toList());
            }

            return new SortResult<>(sortedChunks.get(0),
                    System.currentTimeMillis() - start,
                    effectiveThreads,
                    initialChunks);
        } finally {
            executor.shutdown();
        }
    }

    /** Разбивает список на n примерно равных частей. */
    private static <T> List<List<T>> splitIntoChunks(List<T> input, int n) {
        int size = input.size();
        int base = size / n;
        int remainder = size % n;
        List<List<T>> chunks = new ArrayList<>(n);
        int idx = 0;
        for (int i = 0; i < n; i++) {
            int chunkSize = base + (i < remainder ? 1 : 0);
            if (chunkSize == 0) break;
            chunks.add(new ArrayList<>(input.subList(idx, idx + chunkSize)));
            idx += chunkSize;
        }
        return chunks;
    }

    /** Последовательная сортировка слиянием (рекурсивная). */
    private static <T> void sequentialMergeSort(List<T> list, Comparator<? super T> cmp) {
        if (list.size() < 2) return;
        if (list.size() <= 32) {           // мелкие массивы — встроенной сортировкой
            list.sort(cmp);
            return;
        }
        int mid = list.size() / 2;
        List<T> left = new ArrayList<>(list.subList(0, mid));
        List<T> right = new ArrayList<>(list.subList(mid, list.size()));
        sequentialMergeSort(left, cmp);
        sequentialMergeSort(right, cmp);
        List<T> merged = merge(left, right, cmp);
        for (int i = 0; i < merged.size(); i++) list.set(i, merged.get(i));
    }

    /** Слияние двух отсортированных списков. */
    private static <T> List<T> merge(List<T> left, List<T> right, Comparator<? super T> cmp) {
        List<T> result = new ArrayList<>(left.size() + right.size());
        int i = 0, j = 0;
        while (i < left.size() && j < right.size()) {
            if (cmp.compare(left.get(i), right.get(j)) <= 0) {
                result.add(left.get(i++));
            } else {
                result.add(right.get(j++));
            }
        }
        while (i < left.size()) result.add(left.get(i++));
        while (j < right.size()) result.add(right.get(j++));
        return result;
    }

    /** Демо из задания — для проверки руками. */
    public static void main(String[] args) {
        List<Integer> ids = List.of(3, 2, 6, 9, 2, 3, 5, 8, 2, 1, 11);
        SortResult<Integer> result = sort(ids, Integer::compareTo, 5);
        System.out.println("Sorted: " + result.sorted());
        System.out.println("Time: " + result.durationMs() + " ms, threads: " + result.threadsUsed());
    }
}