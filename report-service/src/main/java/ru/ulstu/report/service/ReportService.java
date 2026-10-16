package ru.ulstu.report.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.ulstu.report.client.CoreServiceClient;
import ru.ulstu.report.service.MinioService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final CoreServiceClient coreServiceClient;
    private final MinioService minioService;

     private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public List<Map<String, Object>> getAllComputersReport() {
        
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

    public Map<String, Object> exportStatusReport() {
        Map<String, Object> status = getStatusReport();

        StringBuilder csv = new StringBuilder();
        csv.append("metric,value\n");
        csv.append("availableCount,").append(status.get("availableCount")).append("\n");
        csv.append("soldCount,").append(status.get("soldCount")).append("\n");
        csv.append("totalCount,").append(status.get("totalCount")).append("\n");

        String filename = "status-report-" + LocalDateTime.now().format(TS) + ".csv";
        try {
            String objectName = minioService.uploadFile(filename, csv.toString().getBytes());
            return Map.of(
                    "objectName", objectName,
                    "filename", filename,
                    "size", csv.length()
            );
        } catch (Exception e) {
            log.error("Ошибка загрузки в MinIO", e);
            throw new RuntimeException("Не удалось сохранить отчёт", e);
        }
    }

    public Map<String, Object> exportComputersReport() {
        List<Map<String, Object>> computers = getAllComputersReport();

        StringBuilder csv = new StringBuilder();
        csv.append("id,model,statusCode,statusName,currentPrice\n");
        for (Map<String, Object> c : computers) {
            csv.append(csvEscape(c.get("id"))).append(",")
               .append(csvEscape(c.get("model"))).append(",")
               .append(csvEscape(c.get("statusCode"))).append(",")
               .append(csvEscape(c.get("statusName"))).append(",")
               .append(csvEscape(c.get("currentPrice"))).append("\n");
        }

        String filename = "computers-report-" + LocalDateTime.now().format(TS) + ".csv";
        try {
            String objectName = minioService.uploadFile(filename, csv.toString().getBytes());
            return Map.of(
                    "objectName", objectName,
                    "filename", filename,
                    "size", csv.length()
            );
        } catch (Exception e) {
            log.error("Ошибка загрузки в MinIO", e);
            throw new RuntimeException("Не удалось сохранить отчёт", e);
        }
    }

    // === Скачивание ===

    public byte[] downloadFile(String objectName) {
        try {
            return minioService.downloadFile(objectName).readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Файл не найден: " + objectName, e);
        }
    }

    private String csvEscape(Object value) {
        if (value == null) return "";
        String s = value.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

}