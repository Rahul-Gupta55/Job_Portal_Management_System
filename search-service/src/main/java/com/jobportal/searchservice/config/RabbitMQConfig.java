package com.jobportal.searchservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "jobportal.exchange";
    public static final String JOB_UPSERT_QUEUE = "search.job.upsert.queue";
    public static final String JOB_DELETE_QUEUE = "search.job.delete.queue";

    @Bean
    public TopicExchange searchExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue jobUpsertQueue() {
        return QueueBuilder.durable(JOB_UPSERT_QUEUE).build();
    }

    @Bean
    public Queue jobDeleteQueue() {
        return QueueBuilder.durable(JOB_DELETE_QUEUE).build();
    }

    @Bean
    public Binding jobUpsertBinding(Queue jobUpsertQueue, TopicExchange exchange) {
        return BindingBuilder.bind(jobUpsertQueue).to(exchange).with("job.upserted");
    }

    @Bean
    public Binding jobDeleteBinding(Queue jobDeleteQueue, TopicExchange exchange) {
        return BindingBuilder.bind(jobDeleteQueue).to(exchange).with("job.deleted");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
