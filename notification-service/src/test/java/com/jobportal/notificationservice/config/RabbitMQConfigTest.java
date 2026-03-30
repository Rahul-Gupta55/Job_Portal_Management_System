package com.jobportal.notificationservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RabbitMQConfigTest {

    private final RabbitMQConfig config = new RabbitMQConfig();

    @Test
    void exchangeIsDurableTopicExchange() {
        TopicExchange exchange = config.exchange();
        assertThat(exchange.getName()).isEqualTo(RabbitMQConfig.EXCHANGE);
        assertThat(exchange.isDurable()).isTrue();
    }

    @Test
    void queuesUseExpectedNames() {
        Queue created = config.jobCreatedQueue();
        Queue applied = config.jobAppliedQueue();
        Queue closed = config.jobClosedQueue();
        Queue status = config.appStatusQueue();

        assertThat(created.getName()).isEqualTo(RabbitMQConfig.Q_JOB_CREATED);
        assertThat(applied.getName()).isEqualTo(RabbitMQConfig.Q_JOB_APPLIED);
        assertThat(closed.getName()).isEqualTo(RabbitMQConfig.Q_JOB_CLOSED);
        assertThat(status.getName()).isEqualTo(RabbitMQConfig.Q_APP_STATUS);
    }

    @Test
    void bindingsUseExpectedRoutingKeys() {
        Binding createdBinding = config.jobCreatedBinding(config.jobCreatedQueue(), config.exchange());
        Binding appliedBinding = config.jobAppliedBinding(config.jobAppliedQueue(), config.exchange());
        Binding closedBinding = config.jobClosedBinding(config.jobClosedQueue(), config.exchange());
        Binding statusBinding = config.appStatusBinding(config.appStatusQueue(), config.exchange());

        assertThat(createdBinding.getRoutingKey()).isEqualTo("job.created");
        assertThat(appliedBinding.getRoutingKey()).isEqualTo("job.applied");
        assertThat(closedBinding.getRoutingKey()).isEqualTo("job.closed");
        assertThat(statusBinding.getRoutingKey()).isEqualTo("app.status.changed");
    }

    @Test
    void jsonMessageConverterUsesJackson() {
        assertThat(config.jsonMessageConverter()).isInstanceOf(Jackson2JsonMessageConverter.class);
    }

    @Test
    void amqpTemplateUsesJacksonConverter() {
        AmqpTemplate template = config.amqpTemplate(mock(ConnectionFactory.class));
        assertThat(((RabbitTemplate) template).getMessageConverter()).isInstanceOf(Jackson2JsonMessageConverter.class);
    }
}
