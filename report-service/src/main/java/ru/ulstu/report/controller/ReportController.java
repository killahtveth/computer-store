package ru.ulstu.report.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.ulstu.report.dto.ComputerReportItem;
import ru.ulstu.report.dto.StatusReport;
import ru.ulstu.report.entity.ComputerPriceHistory;
import ru.ulstu.report.service.ReportService;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/computers")
    public ResponseEntity<List<ComputerReportItem>> getAllComputers() {
        return ResponseEntity.ok(reportService.getAllComputersReport());
    }

    @GetMapping("/status")
    public ResponseEntity<StatusReport> getStatusReport() {
        return ResponseEntity.ok(reportService.getStatusReport());
    }

    @GetMapping("/price-history/{computerId}")
    public ResponseEntity<List<ComputerPriceHistory>> getPriceHistory(@PathVariable Long computerId) {
        return ResponseEntity.ok(reportService.getPriceHistory(computerId));
    }
}