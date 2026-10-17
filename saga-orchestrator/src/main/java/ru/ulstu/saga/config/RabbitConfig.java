package ru.ulstu.saga.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.ExchangeBuilder;

@Configuration
public class RabbitConfig {

    // === Exchange и DLQ (общие для всей саги) ===
    public static final String SAGA_EXCHANGE = "saga.exchange";
    public static final String DLX = "saga.dlx";
    public static final String DLQ = "saga.dlq";

    // === Старт саги: core-service → orchestrator ===
    public static final String SAGA_START_QUEUE = "saga.start.queue";
    public static final String SAGA_START_KEY   = "saga.start";

    // === Команда: orchestrator → report-service (шаг 1) ===
    public static final String REPORT_UPLOAD_COMMAND_QUEUE = "report.upload.command.queue";
    public static final String REPORT_UPLOAD_COMMAND_KEY   = "report.upload.command";

    // === Команда: orchestrator → report-service (компенсация) ===
    public static final String REPORT_DELETE_COMMAND_QUEUE = "report.delete.command.queue";
    public static final String REPORT_DELETE_COMMAND_KEY   = "report.delete.command";

    // === Команда: orchestrator → notification-service (шаг 2) ===
    public static final String NOTIFY_COMMAND_QUEUE = "notification.send.command.queue";
    public static final String NOTIFY_COMMAND_KEY   = "notification.send.command";

    // === Результаты шага 1 (report-service → orchestrator) ===
    public static final String REPORT_UPLOADED_KEY     = "report.uploaded";
    public static final String REPORT_UPLOAD_FAILED_KEY = "report.upload.failed";
    public static final String REPORT_DELETED_KEY      = "report.deleted";
    public static final String REPORT_DELETE_FAILED_KEY = "report.delete.failed";

    // === Результаты шага 2 (notification-service → orchestrator) ===
    public static final String NOTIFICATION_SENT_KEY   = "notification.sent";
    public static final String NOTIFICATION_FAILED_KEY = "notification.failed";

    // === Единая очередь для всех результатов (orchestrator слушает её) ===
    public static final String SAGA_RESULT_QUEUE = "saga.result.queue";

    // === Exchanges ===
    @Bean
    public TopicExchange sagaExchange() {
        return ExchangeBuilder.topicExchange(SAGA_EXCHANGE).durable(true).build();
    }

    @Bean
    public DirectExchange sagaDlx() {
        return ExchangeBuilder.directExchange(DLX).durable(true).build();
    }

    // === Очереди ===
    @Bean
    public Queue sagaStartQueue() {
        return QueueBuilder.durable(SAGA_START_QUEUE)
                .deadLetterExchange(DLX).deadLetterRoutingKey(DLQ).build();
    }

    @Bean
    public Queue reportUploadCommandQueue() {
        return QueueBuilder.durable(REPORT_UPLOAD_COMMAND_QUEUE)
                .deadLetterExchange(DLX).deadLetterRoutingKey(DLQ).build();
    }

    @Bean
    public Queue reportDeleteCommandQueue() {
        return QueueBuilder.durable(REPORT_DELETE_COMMAND_QUEUE)
                .deadLetterExchange(DLX).deadLetterRoutingKey(DLQ).build();
    }

    @Bean
    public Queue notifyCommandQueue() {
        return QueueBuilder.durable(NOTIFY_COMMAND_QUEUE)
                .deadLetterExchange(DLX).deadLetterRoutingKey(DLQ).build();
    }

    @Bean
    public Queue sagaResultQueue() {
        return QueueBuilder.durable(SAGA_RESULT_QUEUE)
                .deadLetterExchange(DLX).deadLetterRoutingKey(DLQ).build();
    }

    @Bean
    public Queue sagaDlq() {
        return QueueBuilder.durable(DLQ).build();
    }

    // === Bindings ===
    @Bean
    public Binding sagaStartBinding() {
        return BindingBuilder.bind(sagaStartQueue()).to(sagaExchange()).with(SAGA_START_KEY);
    }

    @Bean
    public Binding reportUploadCommandBinding() {
        return BindingBuilder.bind(reportUploadCommandQueue()).to(sagaExchange()).with(REPORT_UPLOAD_COMMAND_KEY);
    }

    @Bean
    public Binding reportDeleteCommandBinding() {
        return BindingBuilder.bind(reportDeleteCommandQueue()).to(sagaExchange()).with(REPORT_DELETE_COMMAND_KEY);
    }

    @Bean
    public Binding notifyCommandBinding() {
        return BindingBuilder.bind(notifyCommandQueue()).to(sagaExchange()).with(NOTIFY_COMMAND_KEY);
    }

    @Bean
    public Binding sagaUploadedBinding() {
        return BindingBuilder.bind(sagaResultQueue()).to(sagaExchange()).with(REPORT_UPLOADED_KEY);
    }

    @Bean
    public Binding sagaUploadFailedBinding() {
        return BindingBuilder.bind(sagaResultQueue()).to(sagaExchange()).with(REPORT_UPLOAD_FAILED_KEY);
    }

    @Bean
    public Binding sagaDeletedBinding() {
        return BindingBuilder.bind(sagaResultQueue()).to(sagaExchange()).with(REPORT_DELETED_KEY);
    }

    @Bean
    public Binding sagaDeleteFailedBinding() {
        return BindingBuilder.bind(sagaResultQueue()).to(sagaExchange()).with(REPORT_DELETE_FAILED_KEY);
    }

    @Bean
    public Binding sagaNotifiedBinding() {
        return BindingBuilder.bind(sagaResultQueue()).to(sagaExchange()).with(NOTIFICATION_SENT_KEY);
    }

    @Bean
    public Binding sagaNotifyFailedBinding() {
        return BindingBuilder.bind(sagaResultQueue()).to(sagaExchange()).with(NOTIFICATION_FAILED_KEY);
    }

    @Bean
    public Binding sagaDlqBinding() {
        return BindingBuilder.bind(sagaDlq()).to(sagaDlx()).with(DLQ);
    }

    // === Converter и Template ===
    @Bean
public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
    Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
    DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
    typeMapper.setTypePrecedence(DefaultJackson2JavaTypeMapper.TypePrecedence.INFERRED);
    typeMapper.setTrustedPackages("*");
    converter.setJavaTypeMapper(typeMapper);
    return converter;
}

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        template.setMandatory(true);
        return template;
    }
}