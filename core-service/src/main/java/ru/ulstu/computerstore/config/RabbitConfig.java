package ru.ulstu.computerstore.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // === Существующий report exchange (lab5) ===
    public static final String EXCHANGE = "report.exchange";
    public static final String STATUS_ROUTING_KEY = "report.export.status";
    public static final String COMPUTERS_ROUTING_KEY = "report.export.computers";

    public static final String STATUS_QUEUE = "report.export.status.queue";
    public static final String COMPUTERS_QUEUE = "report.export.computers.queue";
    public static final String SAGA_START_QUEUE = "saga.start.queue";

    public static final String DLX = "report.dlx";
    public static final String DLQ = "report.dlq";

    // === Saga exchange (lab5 доп) ===
    public static final String SAGA_EXCHANGE = "saga.exchange";
    public static final String SAGA_START_KEY = "saga.start";
    public static final String SAGA_DLX = "saga.dlx";
    public static final String SAGA_DLQ = "saga.dlq";

    // === report.exchange сущности ===
    @Bean
    public TopicExchange reportExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(DLX).durable(true).build();
    }

    @Bean
    public Queue statusQueue() {
        return QueueBuilder.durable(STATUS_QUEUE)
                .deadLetterExchange(DLX).deadLetterRoutingKey(DLQ).build();
    }

    @Bean
    public Queue computersQueue() {
        return QueueBuilder.durable(COMPUTERS_QUEUE)
                .deadLetterExchange(DLX).deadLetterRoutingKey(DLQ).build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Binding statusBinding() {
        return BindingBuilder.bind(statusQueue()).to(reportExchange()).with(STATUS_ROUTING_KEY);
    }

    @Bean
    public Binding computersBinding() {
        return BindingBuilder.bind(computersQueue()).to(reportExchange()).with(COMPUTERS_ROUTING_KEY);
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(DLQ);
    }

    // === saga.exchange сущности ===
    @Bean
    public TopicExchange sagaExchange() {
        return ExchangeBuilder.topicExchange(SAGA_EXCHANGE).durable(true).build();
    }

    @Bean
    public DirectExchange sagaDlx() {
        return ExchangeBuilder.directExchange(SAGA_DLX).durable(true).build();
    }

    @Bean
    public Queue sagaDlq() {
        return QueueBuilder.durable(SAGA_DLQ).build();
    }

    @Bean
    public Binding sagaDlqBinding() {
        return BindingBuilder.bind(sagaDlq()).to(sagaDlx()).with(SAGA_DLQ);
    }

    // === Общие бины ===
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

    @Bean
    public Queue sagaStartQueue() {
        return QueueBuilder.durable(SAGA_START_QUEUE)
                .deadLetterExchange(SAGA_DLX).deadLetterRoutingKey(SAGA_DLQ).build();
    }

    @Bean
    public Binding sagaStartBinding() {
        return BindingBuilder.bind(sagaStartQueue()).to(sagaExchange()).with(SAGA_START_KEY);
    }
}