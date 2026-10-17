package ru.ulstu.computerstore.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import ru.ulstu.computerstore.service.ReportFileAsyncService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/reports/files")
@RequiredArgsConstructor
public class ReportFileAsyncController {

    private final ReportFileAsyncService fileService;
    private final RestTemplate restTemplate;

    @Value("${report.service.url}")
    private String reportServiceUrl;

    // === 1. Простой async: список файлов ===

    @GetMapping("/async")
    public CompletableFuture<ResponseEntity<List<Map<String, Object>>>> listAsync() {
        return fileService.listFilesAsync()
                .thenApply(ResponseEntity::ok)
                .orTimeout(10, TimeUnit.SECONDS)
                .exceptionally(e -> {
                    log.error("Ошибка async listFiles", e);
                    return ResponseEntity.status(504).body(null);
                });
    }

    // === 2. Async: скачивание файла ===

    @GetMapping("/async/{*objectName}")
    public CompletableFuture<ResponseEntity<byte[]>> downloadAsync(@PathVariable String objectName) {
        if (objectName.startsWith("/")) {
            objectName = objectName.substring(1);
        }
        final String name = objectName;

        return fileService.downloadFileAsync(name)
                .thenApply(bytes -> ResponseEntity.ok()
                        .header("Content-Disposition", "attachment; filename=\"" + name + "\"")
                        .header("Content-Type", "text/csv")
                        .body(bytes))
                .orTimeout(10, TimeUnit.SECONDS)
                .exceptionally(e -> {
                    log.error("Ошибка async download {}", name, e);
                    return ResponseEntity.status(504).body(null);
                });
    }

    // === 3. Параллельная загрузка нескольких файлов через allOf ===

    @GetMapping("/batch")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> batch(
            @RequestParam List<String> names) {

        long start = System.currentTimeMillis();
        log.info(">>> batch download для {} файлов", names.size());

        // по одному CompletableFuture на файл
        List<CompletableFuture<Map.Entry<String, Integer>>> futures = names.stream()
                .map(name -> fileService.downloadFileAsync(name)
                        .thenApply(bytes -> Map.entry(name, bytes.length))
                        .exceptionally(e -> {
                            log.error("Ошибка загрузки {}", name, e);
                            return Map.entry(name, -1);
                        }))
                .toList();

        // allOf — ждём все сразу, а не по одному
        CompletableFuture<?>[] array = futures.toArray(new CompletableFuture[0]);

        return CompletableFuture.allOf(array)
                .thenApply(v -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    for (var f : futures) {
                        var entry = f.join();
                        result.put(entry.getKey(),
                                entry.getValue() >= 0
                                        ? entry.getValue() + " bytes"
                                        : "error");
                    }
                    result.put("totalTimeMs", System.currentTimeMillis() - start);
                    return ResponseEntity.ok(result);
                })
                .orTimeout(30, TimeUnit.SECONDS)
                .exceptionally(e -> {
                    log.error("Ошибка batch", e);
                    return ResponseEntity.status(504).body(Map.of("error", "timeout"));
                });
    }

    // === 4. Композиция: список + параллельная загрузка всех + агрегация ===

    @GetMapping("/aggregate")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> aggregate() {
        long start = System.currentTimeMillis();

        // thenCompose: результат первого шага передаётся во второй
        return fileService.listFilesAsync()
                .thenCompose(files -> {
                    if (files == null || files.isEmpty()) {
                        return CompletableFuture.completedFuture(
                                ResponseEntity.ok(Map.<String, Object>of(
                                        "fileCount", 0,
                                        "totalSize", 0L,
                                        "totalTimeMs", System.currentTimeMillis() - start
                                ))
                        );
                    }

                    // параллельно качаем все файлы
                    List<CompletableFuture<Integer>> downloads = files.stream()
                            .map(f -> fileService.downloadFileAsync((String) f.get("name"))
                                    .thenApply(b -> b.length)
                                    .exceptionally(e -> 0))
                            .toList();

                    CompletableFuture<?>[] array = downloads.toArray(new CompletableFuture[0]);

                    return CompletableFuture.allOf(array)
                            .thenApply(v -> {
                                long totalSize = downloads.stream()
                                        .mapToLong(CompletableFuture::join)
                                        .sum();

                                Map<String, Object> result = new LinkedHashMap<>();
                                result.put("fileCount", files.size());
                                result.put("totalSize", totalSize);
                                result.put("files", files);
                                result.put("totalTimeMs", System.currentTimeMillis() - start);
                                return ResponseEntity.ok(result);
                            });
                })
                .orTimeout(60, TimeUnit.SECONDS)
                .exceptionally(e -> {
                    log.error("Ошибка aggregate", e);
                    return ResponseEntity.status(504).body(Map.of("error", "timeout"));
                });
    }

    // === 5. Benchmark: sync vs async — для защиты ===

    @GetMapping("/benchmark")
    public ResponseEntity<Map<String, Object>> benchmark(
            @RequestParam(defaultValue = "3") int parallel) {

        // получаем список синхронно (нужны имена)
        List<Map<String, Object>> files = restTemplate.exchange(
                reportServiceUrl + "/internal/reports/files",
                HttpMethod.GET,
                null,
                new org.springframework.core.ParameterizedTypeReference<List<Map<String, Object>>>() {}
        ).getBody();

        if (files == null || files.isEmpty()) {
            return ResponseEntity.ok(Map.of("error", "нет файлов в хранилище"));
        }

        List<String> names = files.stream()
                .limit(parallel)
                .map(f -> (String) f.get("name"))
                .toList();

        // === SYNCHRONOUS: качаем по очереди ===
        long syncStart = System.currentTimeMillis();
        for (String name : names) {
            try {
                restTemplate.exchange(
                        reportServiceUrl + "/internal/reports/files/" + name,
                        HttpMethod.GET, null, byte[].class);
            } catch (Exception e) {
                log.warn("sync download {} failed", name, e);
            }
        }
        long syncMs = System.currentTimeMillis() - syncStart;

        // === ASYNCHRONOUS: качаем параллельно ===
        long asyncStart = System.currentTimeMillis();
        List<CompletableFuture<byte[]>> futures = names.stream()
                .map(fileService::downloadFileAsync)
                .toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        long asyncMs = System.currentTimeMillis() - asyncStart;

        double speedup = asyncMs > 0 ? (double) syncMs / asyncMs : 0;

        return ResponseEntity.ok(Map.of(
                "fileCount", names.size(),
                "syncMs", syncMs,
                "asyncMs", asyncMs,
                "speedup", Math.round(speedup * 100.0) / 100.0
        ));
    }
}