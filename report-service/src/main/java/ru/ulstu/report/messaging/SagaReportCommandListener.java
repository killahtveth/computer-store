package ru.ulstu.report.messaging;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import ru.ulstu.report.config.RabbitConfig;
import ru.ulstu.report.service.MinioService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class SagaReportCommandListener {

    private final MinioService minioService;
    private final RabbitTemplate rabbitTemplate;

    // === Шаг 1: загрузка отчёта в MinIO ===

    @RabbitListener(queues = RabbitConfig.REPORT_UPLOAD_COMMAND_QUEUE)
    public void onUploadCommand(SagaMessage msg,
                                Channel channel,
                                @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        String traceId = msg.traceId();
        if (traceId != null) MDC.put("traceId", traceId);

        try {
            String objectName = "saga-report-" + msg.sagaId() + ".csv";
            log.info(">>> Получена команда на upload саги {} (object={})",
                    msg.sagaId(), objectName);

            // Идемпотентность: если файл уже есть — пропускаем upload
            if (!minioService.objectExists(objectName)) {
                String csv = buildStatusCsv(msg);
                minioService.uploadFile(objectName, csv.getBytes(StandardCharsets.UTF_8));
            } else {
                log.info("Файл {} уже существует — пропускаем upload", objectName);
            }

            SagaMessage reply = new SagaMessage(
                    msg.sagaId(),
                    msg.requestId(),
                    msg.traceId(),
                    msg.reportType(),
                    msg.status(),
                    msg.computers(),
                    objectName,
                    null
            );

            rabbitTemplate.convertAndSend(
                    RabbitConfig.SAGA_EXCHANGE,
                    RabbitConfig.REPORT_UPLOADED_KEY,
                    reply
            );

            channel.basicAck(deliveryTag, false);
            log.info("<<< Uploaded, ответ отправлен в saga.exchange ({}), сага {}",
                    RabbitConfig.REPORT_UPLOADED_KEY, msg.sagaId());

        } catch (Exception e) {
            log.error("Ошибка upload для саги {}", msg.sagaId(), e);

            SagaMessage reply = new SagaMessage(
                    msg.sagaId(), msg.requestId(), msg.traceId(), msg.reportType(),
                    msg.status(), msg.computers(), null, e.getMessage()
            );

            rabbitTemplate.convertAndSend(
                    RabbitConfig.SAGA_EXCHANGE,
                    RabbitConfig.REPORT_UPLOAD_FAILED_KEY,
                    reply
            );

            channel.basicAck(deliveryTag, false); // бизнес-ошибку уже обработали, не в DLQ
        } finally {
            MDC.remove("traceId");
        }
    }

    // === Компенсация: удаление файла из MinIO ===

    @RabbitListener(queues = RabbitConfig.REPORT_DELETE_COMMAND_QUEUE)
    public void onDeleteCommand(SagaMessage msg,
                                Channel channel,
                                @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        String traceId = msg.traceId();
        if (traceId != null) MDC.put("traceId", traceId);

        try {
            String objectName = msg.objectName();
            log.info(">>> Получена команда на delete саги {} (object={})",
                    msg.sagaId(), objectName);

            if (objectName == null) {
                throw new IllegalStateException("objectName == null");
            }

            // Идемпотентность: удаляем только если существует
            if (minioService.objectExists(objectName)) {
                minioService.removeFile(objectName);
            } else {
                log.info("Файл {} уже отсутствует — пропускаем delete", objectName);
            }

            SagaMessage reply = new SagaMessage(
                    msg.sagaId(), msg.requestId(), msg.traceId(), msg.reportType(),
                    msg.status(), msg.computers(), objectName, null
            );

            rabbitTemplate.convertAndSend(
                    RabbitConfig.SAGA_EXCHANGE,
                    RabbitConfig.REPORT_DELETED_KEY,
                    reply
            );

            channel.basicAck(deliveryTag, false);
            log.info("<<< Deleted, ответ отправлен в saga.exchange ({}), сага {}",
                    RabbitConfig.REPORT_DELETED_KEY, msg.sagaId());

        } catch (Exception e) {
            log.error("Ошибка delete для саги {}", msg.sagaId(), e);

            SagaMessage reply = new SagaMessage(
                    msg.sagaId(), msg.requestId(), msg.traceId(), msg.reportType(),
                    msg.status(), msg.computers(), msg.objectName(), e.getMessage()
            );

            rabbitTemplate.convertAndSend(
                    RabbitConfig.SAGA_EXCHANGE,
                    RabbitConfig.REPORT_DELETE_FAILED_KEY,
                    reply
            );

            channel.basicAck(deliveryTag, false);
        } finally {
            MDC.remove("traceId");
        }
    }

    // === Сборка CSV ===

    private String buildStatusCsv(SagaMessage msg) {
        if (msg.status() == null) {
            return "metric,value\n";
        }
        StringBuilder sb = new StringBuilder("metric,value\n");
        msg.status().forEach((k, v) -> sb.append(k).append(",").append(v).append("\n"));
        return sb.toString();
    }
}