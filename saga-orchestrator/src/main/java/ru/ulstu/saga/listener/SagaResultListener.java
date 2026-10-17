package ru.ulstu.saga.listener;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import ru.ulstu.saga.config.RabbitConfig;
import ru.ulstu.saga.messaging.SagaMessage;
import ru.ulstu.saga.service.SagaOrchestratorService;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class SagaResultListener {

    private final SagaOrchestratorService orchestrator;

    @RabbitListener(queues = RabbitConfig.SAGA_RESULT_QUEUE)
    public void onResult(SagaMessage msg,
                         @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey,
                         Channel channel,
                         @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        if (msg.traceId() != null) MDC.put("traceId", msg.traceId());

        try {
            log.info("<<< saga.result: routingKey={}, sagaId={}", routingKey, msg.sagaId());

            switch (routingKey) {
                case RabbitConfig.REPORT_UPLOADED_KEY      -> orchestrator.onReportUploaded(msg);
                case RabbitConfig.REPORT_UPLOAD_FAILED_KEY -> orchestrator.onReportUploadFailed(msg);
                case RabbitConfig.REPORT_DELETED_KEY       -> orchestrator.onReportDeleted(msg);
                case RabbitConfig.REPORT_DELETE_FAILED_KEY -> orchestrator.onReportDeleteFailed(msg);
                case RabbitConfig.NOTIFICATION_SENT_KEY    -> orchestrator.onNotificationSent(msg);
                case RabbitConfig.NOTIFICATION_FAILED_KEY  -> orchestrator.onNotificationFailed(msg);
                default -> log.warn("Неизвестный routingKey: {}", routingKey);
            }

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Ошибка обработки результата {} для саги {}", routingKey, msg.sagaId(), e);
            channel.basicNack(deliveryTag, false, false);
        } finally {
            MDC.remove("traceId");
        }
    }
}