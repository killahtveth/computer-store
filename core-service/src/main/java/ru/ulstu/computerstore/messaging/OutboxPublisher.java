package ru.ulstu.computerstore.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.ulstu.computerstore.config.RabbitConfig;
import ru.ulstu.computerstore.entity.OutboxEvent;
import ru.ulstu.computerstore.repository.OutboxEventRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publish() {
        List<OutboxEvent> events =
                outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc("NEW");

        for (OutboxEvent event : events) {
            try {
                String exchange;
                String routingKey;
                Object payload;

                switch (event.getEventType()) {
                    case "REPORT_EXPORT_STATUS" -> {
                        exchange = RabbitConfig.EXCHANGE;
                        routingKey = RabbitConfig.STATUS_ROUTING_KEY;
                        payload = objectMapper.readValue(event.getPayload(), ReportExportMessage.class);
                    }
                    case "REPORT_EXPORT_COMPUTERS" -> {
                        exchange = RabbitConfig.EXCHANGE;
                        routingKey = RabbitConfig.COMPUTERS_ROUTING_KEY;
                        payload = objectMapper.readValue(event.getPayload(), ReportExportMessage.class);
                    }
                    case "REPORT_EXPORT_FULL" -> {
                        exchange = RabbitConfig.SAGA_EXCHANGE;
                        routingKey = RabbitConfig.SAGA_START_KEY;
                        payload = objectMapper.readValue(event.getPayload(), SagaMessage.class);
                    }
                    default -> throw new IllegalArgumentException(
                            "Неизвестный тип outbox-события: " + event.getEventType());
                }

                CorrelationData correlationData = new CorrelationData(event.getEventId());

                final Object finalPayload = payload;
                rabbitTemplate.convertAndSend(exchange, routingKey, finalPayload,
                        m -> {
                            m.getMessageProperties().setMessageId(event.getEventId());
                            m.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_JSON);
                            return m;
                        },
                        correlationData
                );

                CorrelationData.Confirm confirm =
                        correlationData.getFuture().get(5, TimeUnit.SECONDS);

                if (!confirm.isAck()) {
                    throw new IllegalStateException("RabbitMQ отклонил сообщение: " + event.getEventId());
                }

                event.setStatus("SENT");
                event.setSentAt(LocalDateTime.now());
                event.setError(null);

            } catch (Exception e) {
                log.error("Ошибка публикации outbox-события {}", event.getId(), e);
                event.setRetryCount(event.getRetryCount() + 1);
                event.setError(e.getMessage());

                if (event.getRetryCount() >= 10) {
                    event.setStatus("FAILED");
                }
            }

            outboxEventRepository.save(event);
        }
    }
}