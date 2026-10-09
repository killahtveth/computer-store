package ru.ulstu.computerstore.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

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
        String url = reportServiceUrl + "/internal/reports/computers";
        log.info(">>> [REST] core-service (фасад) вызывает report-service: GET {}", url);
        long start = System.currentTimeMillis();

        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        );

        long duration = System.currentTimeMillis() - start;
        log.info("<<< [REST] Ответ от report-service за {} ms, статус {}",
                duration, response.getStatusCode());

        return ResponseEntity.ok(response.getBody());
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        String url = reportServiceUrl + "/internal/reports/status";
        log.info(">>> [REST] core-service (фасад) вызывает report-service: GET {}", url);
        long start = System.currentTimeMillis();

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        long duration = System.currentTimeMillis() - start;
        log.info("<<< [REST] Ответ от report-service за {} ms, статус {}",
                duration, response.getStatusCode());

        return ResponseEntity.ok(response.getBody());
    }

    @GetMapping("/price-history/{computerId}")
    public ResponseEntity<List<Map<String, Object>>> getPriceHistory(@PathVariable Long computerId) {
        String url = reportServiceUrl + "/internal/reports/price-history/" + computerId;
        log.info(">>> [REST] core-service (фасад) вызывает report-service: GET {}", url);
        long start = System.currentTimeMillis();

        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        );

        long duration = System.currentTimeMillis() - start;
        log.info("<<< [REST] Ответ от report-service за {} ms, статус {}",
                duration, response.getStatusCode());

        return ResponseEntity.ok(response.getBody());
    }
}