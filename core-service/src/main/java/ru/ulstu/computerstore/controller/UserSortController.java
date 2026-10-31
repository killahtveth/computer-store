package ru.ulstu.computerstore.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.ulstu.computerstore.entity.User;
import ru.ulstu.computerstore.service.UserSortService;
import ru.ulstu.sort.ParallelMergeSort;
import ru.ulstu.sort.SortResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserSortController {

    private final UserSortService userSortService;

    /**
     * Сортирует пользователей параллельным merge sort.
     *
     * @param threads число потоков
     * @param sortBy  id | name | email
     * @param limit   сколько вернуть в ответе (чтобы не гнать 10000 строк)
     */
    @GetMapping("/sorted")
    public ResponseEntity<Map<String, Object>> sortedUsers(
            @RequestParam(defaultValue = "4") int threads,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "50") int limit) {

        SortResult<User> result = userSortService.getSortedUsers(sortBy, threads);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sortBy", sortBy);
        response.put("threads", result.threadsUsed());
        response.put("chunks", result.chunkCount());
        response.put("totalCount", result.sorted().size());
        response.put("durationMs", result.durationMs());
        response.put("users", result.sorted().stream().limit(limit).toList());
        return ResponseEntity.ok(response);
    }


    @GetMapping("/sorted/demo")
    public ResponseEntity<Map<String, Object>> demo() {
        List<Integer> ids = List.of(3, 2, 6, 9, 2, 3, 5, 8, 2, 1, 11);
        SortResult<Integer> r = ParallelMergeSort.sort(ids, Integer::compareTo, 5);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("input", ids);
        response.put("sorted", r.sorted());
        response.put("threads", r.threadsUsed());
        response.put("durationMs", r.durationMs());

        return ResponseEntity.ok(response);
    }

    /**
     * Бенчмарк: прогоняет сортировку с разным числом потоков
     * и возвращает сравнение.
     */
    @GetMapping("/sorted/benchmark")
    public ResponseEntity<Map<String, Object>> benchmark(
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "1,2,4,8,16") List<Integer> threadCounts) {

        List<Map<String, Object>> results = new ArrayList<>();
        long baseline = -1;

        for (int t : threadCounts) {
            SortResult<User> r = userSortService.getSortedUsers(sortBy, t);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("threads", r.threadsUsed());
            entry.put("durationMs", r.durationMs());

            if (baseline < 0) baseline = r.durationMs();
            double speedup = r.durationMs() > 0
                    ? Math.round(((double) baseline / r.durationMs()) * 100.0) / 100.0
                    : 0;
            entry.put("speedup", speedup);
            results.add(entry);
        }

        return ResponseEntity.ok(Map.of(
                "size", userSortService.getSortedUsers(sortBy, 1).sorted().size(),
                "results", results
        ));
    }
}