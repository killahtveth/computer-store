package ru.ulstu.report.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.ulstu.report.client.CoreServiceClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final CoreServiceClient coreServiceClient;

    public List<Map<String, Object>> getAllComputersReport() {
        // Список для отчёта получаем через RestTemplate из core-service
        return coreServiceClient.getAllComputers();
    }

    public Map<String, Object> getStatusReport() {
        Map<String, Object> coreReport = coreServiceClient.getCoreReport();
        long available = ((Number) coreReport.get("availableCount")).longValue();
        long sold = ((Number) coreReport.get("soldCount")).longValue();
        return Map.of(
                "availableCount", available,
                "soldCount", sold,
                "totalCount", available + sold
        );
    }

    public List<Map<String, Object>> getPriceHistoryReport(Long computerId) {
    // Получаем сырые данные от core-service
    List<Map<String, Object>> rawHistory = coreServiceClient.getPriceHistory(computerId);
    return rawHistory;
}
}