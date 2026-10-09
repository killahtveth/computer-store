package ru.ulstu.report.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

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
        String url = coreServiceUrl + "/api/computers";
        log.info(">>> [REST] report-service вызывает core-service: GET {}", url);
        long start = System.currentTimeMillis();

        List<Map<String, Object>> result = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        ).getBody();

        log.info("<<< [REST] Ответ от core-service за {} ms",
                System.currentTimeMillis() - start);

        return result;
    }

    public Map<String, Object> getCoreReport() {
        String url = coreServiceUrl + "/api/computers/report";
        log.info(">>> [REST] report-service вызывает core-service: GET {}", url);
        long start = System.currentTimeMillis();

        Map<String, Object> result = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        ).getBody();

        log.info("<<< [REST] Ответ от core-service за {} ms",
                System.currentTimeMillis() - start);

        return result;
    }

    public List<Map<String, Object>> getPriceHistory(Long computerId) {
        String url = coreServiceUrl + "/api/computers/" + computerId + "/price-history";
        log.info(">>> [REST] report-service вызывает core-service: GET {}", url);
        long start = System.currentTimeMillis();

        List<Map<String, Object>> result = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        ).getBody();

        log.info("<<< [REST] Ответ от core-service за {} ms",
                System.currentTimeMillis() - start);

        return result;
    }
}