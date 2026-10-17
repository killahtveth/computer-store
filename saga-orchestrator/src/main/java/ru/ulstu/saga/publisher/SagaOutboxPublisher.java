package ru.ulstu.saga.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.ulstu.saga.config.RabbitConfig;
import ru.ulstu.saga.entity.SagaOutboxEvent;
import ru.ulstu.saga.messaging.SagaMessage;
import ru.ulstu.saga.repository.SagaOutboxEventRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class SagaOutboxPublisher {

    private final SagaOutboxEventRepository outboxRepo;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void publish() {
        List<SagaOutboxEvent> events =
                outboxRepo.findTop100ByStatusOrderByCreatedAtAsc("NEW");

        for (SagaOutboxEvent event : events) {
            try {
                SagaMessage payload = objectMapper.readValue(event.getPayload(), SagaMessage.class);

                CorrelationData correlationData = new CorrelationData(event.getEventId());

                rabbitTemplate.convertAndSend(
                        RabbitConfig.SAGA_EXCHANGE,
                        event.getRoutingKey(),
                        payload,
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
                    throw new IllegalStateException("RabbitMQ NACK: " + event.getEventId());
                }

                event.setStatus("SENT");
                event.setSentAt(LocalDateTime.now());
                event.setError(null);
                log.info("Отправлено saga-событие {} (key={})",
                        event.getEventType(), event.getRoutingKey());

            } catch (Exception e) {
                log.error("Ошибка публикации saga-outbox {}", event.getId(), e);
                event.setRetryCount(event.getRetryCount() + 1);
                event.setError(e.getMessage());
                if (event.getRetryCount() >= 10) {
                    event.setStatus("FAILED");
                }
            }

            outboxRepo.save(event);
        }
    }
}