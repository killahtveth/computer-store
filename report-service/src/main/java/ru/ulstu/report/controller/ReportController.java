package ru.ulstu.report.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.ulstu.report.service.ReportService;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/internal/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

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
}