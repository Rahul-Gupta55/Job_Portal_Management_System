package com.jobportal.applicationservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpTemplate;
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
        TopicExchange exchange = config.applicationExchange();
        assertThat(exchange.getName()).isEqualTo(RabbitMQConfig.EXCHANGE);
        assertThat(exchange.isDurable()).isTrue();
    }

    @Test
    void amqpTemplateUsesJacksonMessageConverter() {
        AmqpTemplate template = config.amqpTemplate(mock(ConnectionFactory.class));
        assertThat(((RabbitTemplate) template).getMessageConverter()).isInstanceOf(Jackson2JsonMessageConverter.class);
    }
}
