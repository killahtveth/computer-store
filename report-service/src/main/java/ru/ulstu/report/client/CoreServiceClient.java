package ru.ulstu.report.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CoreServiceClient {

    private final RestTemplate restTemplate;

    @Value("${core.service.url}")
    private String coreServiceUrl;

    public List<Map<String, Object>> getAllComputers() {
        log.info(">>> report-service вызывает core-service: GET {}/api/computers", coreServiceUrl);
        return restTemplate.exchange(
                coreServiceUrl + "/api/computers",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        ).getBody();
    }

    public Map<String, Object> getCoreReport() {
        log.info(">>> report-service вызывает core-service: GET {}/api/computers/report", coreServiceUrl);
        return restTemplate.exchange(
                coreServiceUrl + "/api/computers/report",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        ).getBody();
    }

    public List<Map<String, Object>> getPriceHistory(Long computerId) {
        log.info(">>> report-service вызывает core-service: GET {}/api/computers/{}/price-history", coreServiceUrl, computerId);
    return restTemplate.exchange(
            coreServiceUrl + "/api/computers/" + computerId + "/price-history",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<Map<String, Object>>>() {}
    ).getBody();
}
}