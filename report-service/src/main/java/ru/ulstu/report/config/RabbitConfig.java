package ru.ulstu.report.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "report.exchange";
    public static final String STATUS_ROUTING_KEY = "report.export.status";
    public static final String COMPUTERS_ROUTING_KEY = "report.export.computers";

    public static final String STATUS_QUEUE = "report.export.status.queue";
    public static final String COMPUTERS_QUEUE = "report.export.computers.queue";

    public static final String DLX = "report.dlx";
    public static final String DLQ = "report.dlq";

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
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(DLQ)
                .build();
    }

    @Bean
    public Queue computersQueue() {
        return QueueBuilder.durable(COMPUTERS_QUEUE)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(DLQ)
                .build();
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

    @Bean
public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
    Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
    DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
    typeMapper.setTypePrecedence(DefaultJackson2JavaTypeMapper.TypePrecedence.INFERRED);
    converter.setJavaTypeMapper(typeMapper);
    return converter;
}
}