package ru.ulstu.notification.messaging;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import ru.ulstu.notification.config.RabbitConfig;
import ru.ulstu.notification.service.NotificationSenderService;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyCommandListener {

    private final NotificationSenderService sender;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitConfig.NOTIFY_COMMAND_QUEUE)
    public void onNotify(SagaMessage msg,
                         Channel channel,
                         @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        if (msg.traceId() != null) MDC.put("traceId", msg.traceId());

        try {
            log.info(">>> Получена команда notify для саги {} (requestId={})",
                    msg.sagaId(), msg.requestId());

            String text = "Отчёт по саге " + msg.sagaId() + " готов, файл: " + msg.objectName();
            sender.send(msg.sagaId(), msg.requestId(), text);

            SagaMessage reply = new SagaMessage(
                    msg.sagaId(), msg.requestId(), msg.traceId(), msg.reportType(),
                    null, null, msg.objectName(), null
            );
            rabbitTemplate.convertAndSend(
                    RabbitConfig.SAGA_EXCHANGE,
                    RabbitConfig.NOTIFICATION_SENT_KEY,
                    reply
            );
            channel.basicAck(deliveryTag, false);
            log.info("<<< notify.sent отправлен для саги {}", msg.sagaId());

        } catch (Exception e) {
            log.error("Ошибка notify для саги {}", msg.sagaId(), e);

            SagaMessage reply = new SagaMessage(
                    msg.sagaId(), msg.requestId(), msg.traceId(), msg.reportType(),
                    null, null, msg.objectName(), e.getMessage()
            );
            rabbitTemplate.convertAndSend(
                    RabbitConfig.SAGA_EXCHANGE,
                    RabbitConfig.NOTIFICATION_FAILED_KEY,
                    reply
            );
            channel.basicAck(deliveryTag, false);
        } finally {
            MDC.remove("traceId");
        }
    }
}