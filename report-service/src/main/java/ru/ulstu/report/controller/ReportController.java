package ru.ulstu.report.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.ulstu.report.service.ReportService;
import ru.ulstu.report.service.MinioService;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/internal/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final MinioService minioService;

    @GetMapping("/computers")
    public ResponseEntity<List<Map<String, Object>>> getAllComputers() {
        log.info(">>> report-service: получен запрос GET /internal/reports/computers");
        return ResponseEntity.ok(reportService.getAllComputersReport());
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        log.info(">>> report-service: получен запрос GET /internal/reports/status");
        return ResponseEntity.ok(reportService.getStatusReport());
    }

    @GetMapping("/price-history/{computerId}")
public ResponseEntity<List<Map<String, Object>>> getPriceHistory(@PathVariable Long computerId) {
    log.info(">>> report-service: получен запрос GET /internal/reports/price-history/{}", computerId);
    return ResponseEntity.ok(reportService.getPriceHistoryReport(computerId));
    }

    // === ЭКСПОРТ ===

    @PostMapping("/export/status")
    public ResponseEntity<Map<String, Object>> exportStatus() {
        log.info(">>> report-service: экспорт отчёта по статусам в MinIO");
        return ResponseEntity.ok(reportService.exportStatusReport());
    }

    @PostMapping("/export/computers")
    public ResponseEntity<Map<String, Object>> exportComputers() {
        log.info(">>> report-service: экспорт отчёта по компьютерам в MinIO");
        return ResponseEntity.ok(reportService.exportComputersReport());
    }

    // === НОВОЕ: presigned URL ===

    @GetMapping("/files/{*objectName}")
public ResponseEntity<byte[]> downloadFile(@PathVariable String objectName) {
    if (objectName.startsWith("/")) {
        objectName = objectName.substring(1);
    }
    log.info(">>> report-service: скачивание файла {} из MinIO", objectName);
    try {
        byte[] content = minioService.downloadFile(objectName).readAllBytes();
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + objectName + "\"")
                .header("Content-Type", "text/csv")
                .body(content);
    } catch (Exception e) {
        log.error("Ошибка скачивания файла", e);
        throw new RuntimeException("Файл не найден: " + objectName, e);
    }
}
}