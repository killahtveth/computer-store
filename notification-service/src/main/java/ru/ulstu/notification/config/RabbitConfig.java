package ru.ulstu.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String SAGA_EXCHANGE = "saga.exchange";
    public static final String SAGA_DLX = "saga.dlx";
    public static final String SAGA_DLQ = "saga.dlq";

    public static final String NOTIFY_COMMAND_QUEUE = "notification.send.command.queue";
    public static final String NOTIFY_COMMAND_KEY   = "notification.send.command";

    public static final String NOTIFICATION_SENT_KEY   = "notification.sent";
    public static final String NOTIFICATION_FAILED_KEY = "notification.failed";

    @Bean
    public TopicExchange sagaExchange() {
        return ExchangeBuilder.topicExchange(SAGA_EXCHANGE).durable(true).build();
    }

    @Bean
    public DirectExchange sagaDlx() {
        return ExchangeBuilder.directExchange(SAGA_DLX).durable(true).build();
    }

    @Bean
    public Queue notifyCommandQueue() {
        return QueueBuilder.durable(NOTIFY_COMMAND_QUEUE)
                .deadLetterExchange(SAGA_DLX).deadLetterRoutingKey(SAGA_DLQ).build();
    }

    @Bean
    public Queue sagaDlq() {
        return QueueBuilder.durable(SAGA_DLQ).build();
    }

    @Bean
    public Binding notifyCommandBinding() {
        return BindingBuilder.bind(notifyCommandQueue())
                .to(sagaExchange()).with(NOTIFY_COMMAND_KEY);
    }

    @Bean
    public Binding sagaDlqBinding() {
        return BindingBuilder.bind(sagaDlq()).to(sagaDlx()).with(SAGA_DLQ);
    }

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