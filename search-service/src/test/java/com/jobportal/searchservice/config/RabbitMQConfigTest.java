package com.jobportal.searchservice.config;

import org.junit.jupiter.api.Test;
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
        TopicExchange exchange = config.searchExchange();
        assertThat(exchange.getName()).isEqualTo(RabbitMQConfig.EXCHANGE);
        assertThat(exchange.isDurable()).isTrue();
    }

    @Test
    void queuesUseExpectedNames() {
        Queue upsertQueue = config.jobUpsertQueue();
        Queue deleteQueue = config.jobDeleteQueue();

        assertThat(upsertQueue.getName()).isEqualTo(RabbitMQConfig.JOB_UPSERT_QUEUE);
        assertThat(deleteQueue.getName()).isEqualTo(RabbitMQConfig.JOB_DELETE_QUEUE);
    }

    @Test
    void bindingsUseExpectedRoutingKeys() {
        Binding upsertBinding = config.jobUpsertBinding(config.jobUpsertQueue(), config.searchExchange());
        Binding deleteBinding = config.jobDeleteBinding(config.jobDeleteQueue(), config.searchExchange());

        assertThat(upsertBinding.getRoutingKey()).isEqualTo("job.upserted");
        assertThat(deleteBinding.getRoutingKey()).isEqualTo("job.deleted");
    }

    @Test
    void jsonMessageConverterUsesJackson() {
        assertThat(config.jsonMessageConverter()).isInstanceOf(Jackson2JsonMessageConverter.class);
    }

    @Test
    void rabbitTemplateUsesJacksonConverter() {
        RabbitTemplate template = config.rabbitTemplate(mock(ConnectionFactory.class));
        assertThat(template.getMessageConverter()).isInstanceOf(Jackson2JsonMessageConverter.class);
    }
}
