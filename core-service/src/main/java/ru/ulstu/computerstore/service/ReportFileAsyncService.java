package ru.ulstu.computerstore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportFileAsyncService {

    private final RestTemplate restTemplate;

    @Value("${report.service.url}")
    private String reportServiceUrl;

    @Async("reportTaskExecutor")
    public CompletableFuture<List<Map<String, Object>>> listFilesAsync() {
        log.info(">>> [ASYNC] listFiles on thread={}", Thread.currentThread().getName());
        long start = System.currentTimeMillis();

        List<Map<String, Object>> body = restTemplate.exchange(
                reportServiceUrl + "/internal/reports/files",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        ).getBody();

        log.info("<<< [ASYNC] listFiles за {} ms", System.currentTimeMillis() - start);
        return CompletableFuture.completedFuture(body);
    }

    @Async("reportTaskExecutor")
    public CompletableFuture<byte[]> downloadFileAsync(String objectName) {
        log.info(">>> [ASYNC] downloadFile {} on thread={}", objectName, Thread.currentThread().getName());
        long start = System.currentTimeMillis();

        byte[] body = restTemplate.exchange(
                reportServiceUrl + "/internal/reports/files/" + objectName,
                HttpMethod.GET,
                null,
                byte[].class
        ).getBody();

        log.info("<<< [ASYNC] downloadFile {} за {} ms", objectName, System.currentTimeMillis() - start);
        return CompletableFuture.completedFuture(body);
    }
}