package com.emmanuelgabe.portfolio.messaging.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for message queuing infrastructure.
 * Sets up exchanges, queues, bindings, and message converters.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMQConfig {

    private final RabbitMQProperties properties;

    // ========== Exchanges ==========

    @Bean
    public DirectExchange emailExchange() {
        return new DirectExchange(properties.getExchanges().getEmail(), true, false);
    }

    @Bean
    public DirectExchange imageExchange() {
        return new DirectExchange(properties.getExchanges().getImage(), true, false);
    }

    @Bean
    public DirectExchange auditExchange() {
        return new DirectExchange(properties.getExchanges().getAudit(), true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(properties.getExchanges().getDlx(), true, false);
    }

    // ========== Email Queues ==========

    private static final int EMAIL_TTL_MS = 86400000;  // 24 hours
    private static final int IMAGE_TTL_MS = 3600000;   // 1 hour
    private static final int AUDIT_TTL_MS = 86400000;  // 24 hours

    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(properties.getQueues().getEmail())
                .withArgument("x-dead-letter-exchange", properties.getExchanges().getDlx())
                .withArgument("x-dead-letter-routing-key", properties.getRoutingKeys().getEmailDlq())
                .withArgument("x-message-ttl", EMAIL_TTL_MS)
                .build();
    }

    @Bean
    public Queue emailDeadLetterQueue() {
        return QueueBuilder.durable(properties.getQueues().getEmailDlq()).build();
    }

    // ========== Image Queues ==========

    @Bean
    public Queue imageQueue() {
        return QueueBuilder.durable(properties.getQueues().getImage())
                .withArgument("x-dead-letter-exchange", properties.getExchanges().getDlx())
                .withArgument("x-dead-letter-routing-key", properties.getRoutingKeys().getImageDlq())
                .withArgument("x-message-ttl", IMAGE_TTL_MS)
                .build();
    }

    @Bean
    public Queue imageDeadLetterQueue() {
        return QueueBuilder.durable(properties.getQueues().getImageDlq()).build();
    }

    // ========== Audit Queues ==========

    @Bean
    public Queue auditQueue() {
        return QueueBuilder.durable(properties.getQueues().getAudit())
                .withArgument("x-dead-letter-exchange", properties.getExchanges().getDlx())
                .withArgument("x-dead-letter-routing-key", properties.getRoutingKeys().getAuditDlq())
                .withArgument("x-message-ttl", AUDIT_TTL_MS)
                .build();
    }

    @Bean
    public Queue auditDeadLetterQueue() {
        return QueueBuilder.durable(properties.getQueues().getAuditDlq()).build();
    }

    // ========== Email Bindings ==========

    @Bean
    public Binding emailBinding(Queue emailQueue, DirectExchange emailExchange) {
        return BindingBuilder.bind(emailQueue)
                .to(emailExchange)
                .with(properties.getRoutingKeys().getEmail());
    }

    @Bean
    public Binding emailDlqBinding(Queue emailDeadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(emailDeadLetterQueue)
                .to(deadLetterExchange)
                .with(properties.getRoutingKeys().getEmailDlq());
    }

    // ========== Image Bindings ==========

    @Bean
    public Binding imageBinding(Queue imageQueue, DirectExchange imageExchange) {
        return BindingBuilder.bind(imageQueue)
                .to(imageExchange)
                .with(properties.getRoutingKeys().getImage());
    }

    @Bean
    public Binding imageDlqBinding(Queue imageDeadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(imageDeadLetterQueue)
                .to(deadLetterExchange)
                .with(properties.getRoutingKeys().getImageDlq());
    }

    // ========== Audit Bindings ==========

    @Bean
    public Binding auditBinding(Queue auditQueue, DirectExchange auditExchange) {
        return BindingBuilder.bind(auditQueue)
                .to(auditExchange)
                .with(properties.getRoutingKeys().getAudit());
    }

    @Bean
    public Binding auditDlqBinding(Queue auditDeadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(auditDeadLetterQueue)
                .to(deadLetterExchange)
                .with(properties.getRoutingKeys().getAuditDlq());
    }

    // ========== Message Converter ==========

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter
    ) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        template.setMandatory(true);

        // Publisher confirms callback
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("[RABBITMQ] Message not confirmed by broker - correlationData={}, cause={}",
                        correlationData, cause);
            } else {
                log.debug("[RABBITMQ] Message confirmed by broker - correlationData={}", correlationData);
            }
        });

        // Return callback for unroutable messages
        template.setReturnsCallback(returned -> {
            log.error("[RABBITMQ] Message returned - exchange={}, routingKey={}, replyCode={}, replyText={}",
                    returned.getExchange(), returned.getRoutingKey(),
                    returned.getReplyCode(), returned.getReplyText());
        });

        log.info("[RABBITMQ] RabbitTemplate configured with publisher confirms");
        return template;
    }
}
