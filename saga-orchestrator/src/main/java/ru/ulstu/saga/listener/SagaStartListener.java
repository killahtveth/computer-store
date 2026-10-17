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
public class SagaStartListener {

    private final SagaOrchestratorService orchestrator;

    @RabbitListener(queues = RabbitConfig.SAGA_START_QUEUE)
    public void onStart(SagaMessage msg,
                        Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        if (msg.traceId() != null) MDC.put("traceId", msg.traceId());

        try {
            log.info(">>> Получена команда saga.start: sagaId={}, requestId={}",
                    msg.sagaId(), msg.requestId());
            orchestrator.startSaga(msg);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Ошибка старта саги {}", msg.sagaId(), e);
            channel.basicNack(deliveryTag, false, false); // в DLQ
        } finally {
            MDC.remove("traceId");
        }
    }
}