package ru.ulstu.computerstore.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportFacadeController {

    private final RestTemplate restTemplate;

    @Value("${report.service.url}")
    private String reportServiceUrl;

    @GetMapping("/computers")
    public ResponseEntity<List<Map<String, Object>>> getAllComputers() {
        log.info(">>> core-service (фасад) вызывает report-service: GET {}/internal/reports/computers", reportServiceUrl);
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                reportServiceUrl + "/internal/reports/computers",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );
        return ResponseEntity.ok(response.getBody());
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        log.info(">>> core-service (фасад) вызывает report-service: GET {}/internal/reports/status", reportServiceUrl);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                reportServiceUrl + "/internal/reports/status",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );
        return ResponseEntity.ok(response.getBody());
    }

    @GetMapping("/price-history/{computerId}")
    public ResponseEntity<List<Map<String, Object>>> getPriceHistory(@PathVariable Long computerId) {
         log.info(">>> core-service (фасад) вызывает report-service: GET {}/internal/reports/price-history/{}", reportServiceUrl, computerId);
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                reportServiceUrl + "/internal/reports/price-history/" + computerId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );
        return ResponseEntity.ok(response.getBody());
    }
}