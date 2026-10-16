package ru.ulstu.report.messaging;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import ru.ulstu.report.config.RabbitConfig;
import ru.ulstu.report.service.MinioService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportExportListener {

    private final MinioService minioService;

    @RabbitListener(queues = RabbitConfig.STATUS_QUEUE)
    public void handleStatus(ReportExportMessage msg,
                             Channel channel,
                             @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        String traceId = msg.traceId();
        if (traceId != null) {
            MDC.put("traceId", traceId);
        }

        String objectName = "status-report-" + msg.requestId() + ".csv";

        try {
            if (!minioService.objectExists(objectName)) {
                String csv = buildStatusCsv(msg.status());
                minioService.uploadFile(objectName, csv.getBytes(StandardCharsets.UTF_8));
            }

            channel.basicAck(deliveryTag, false);
            log.info("Status-отчёт {} сохранён в MinIO как {}", msg.requestId(), objectName);

        } catch (Exception e) {
            log.error("Ошибка обработки status-отчёта {}", msg.requestId(), e);
            channel.basicNack(deliveryTag, false, false); // в DLQ
        } finally {
            MDC.remove("traceId");
        }
    }

    @RabbitListener(queues = RabbitConfig.COMPUTERS_QUEUE)
    public void handleComputers(ReportExportMessage msg,
                                Channel channel,
                                @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        String traceId = msg.traceId();
        if (traceId != null) {
            MDC.put("traceId", traceId);
        }

        String objectName = "computers-report-" + msg.requestId() + ".csv";

        try {
            if (!minioService.objectExists(objectName)) {
                String csv = buildComputersCsv(msg.computers());
                minioService.uploadFile(objectName, csv.getBytes(StandardCharsets.UTF_8));
            }

            channel.basicAck(deliveryTag, false);
            log.info("Computers-отчёт {} сохранён в MinIO как {}", msg.requestId(), objectName);

        } catch (Exception e) {
            log.error("Ошибка обработки computers-отчёта {}", msg.requestId(), e);
            channel.basicNack(deliveryTag, false, false); // в DLQ
        } finally {
            MDC.remove("traceId");
        }
    }

    private String buildStatusCsv(Map<String, Object> status) {
        return "metric,value\n" +
                "availableCount," + status.get("availableCount") + "\n" +
                "soldCount," + status.get("soldCount") + "\n" +
                "totalCount," + status.get("totalCount") + "\n";
    }

    private String buildComputersCsv(List<Map<String, Object>> computers) {
        StringBuilder csv = new StringBuilder("id,model,statusCode,statusName,currentPrice\n");

        for (Map<String, Object> c : computers) {
            csv.append(csvEscape(c.get("id"))).append(",")
                    .append(csvEscape(c.get("model"))).append(",")
                    .append(csvEscape(c.get("statusCode"))).append(",")
                    .append(csvEscape(c.get("statusName"))).append(",")
                    .append(csvEscape(c.get("currentPrice"))).append("\n");
        }

        return csv.toString();
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