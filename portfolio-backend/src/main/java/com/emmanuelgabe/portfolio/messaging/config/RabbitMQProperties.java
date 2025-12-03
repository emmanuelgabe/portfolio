package com.emmanuelgabe.portfolio.messaging.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for RabbitMQ messaging.
 * Loaded from application.yml under the 'rabbitmq' prefix.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "rabbitmq")
public class RabbitMQProperties {

    private boolean enabled = true;

    private Queues queues = new Queues();
    private Exchanges exchanges = new Exchanges();
    private RoutingKeys routingKeys = new RoutingKeys();

    @Getter
    @Setter
    public static class Queues {
        private String email = "portfolio.email.queue";
        private String emailDlq = "portfolio.email.dlq";
        private String image = "portfolio.image.queue";
        private String imageDlq = "portfolio.image.dlq";
        private String audit = "portfolio.audit.queue";
        private String auditDlq = "portfolio.audit.dlq";
    }

    @Getter
    @Setter
    public static class Exchanges {
        private String email = "portfolio.email.exchange";
        private String image = "portfolio.image.exchange";
        private String audit = "portfolio.audit.exchange";
        private String dlx = "portfolio.dlx.exchange";
    }

    @Getter
    @Setter
    public static class RoutingKeys {
        private String email = "email.send";
        private String emailDlq = "email.dlq";
        private String image = "image.process";
        private String imageDlq = "image.dlq";
        private String audit = "audit.log";
        private String auditDlq = "audit.dlq";
    }
}
