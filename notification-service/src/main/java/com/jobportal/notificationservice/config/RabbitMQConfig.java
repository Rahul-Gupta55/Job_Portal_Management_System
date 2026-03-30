package com.jobportal.notificationservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.*;
import org.springframework.context.annotation.*;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "jobportal.exchange";
    public static final String Q_JOB_CREATED = "job.created.queue";
    public static final String Q_JOB_APPLIED = "job.applied.queue";
    public static final String Q_JOB_CLOSED = "job.closed.queue";
    public static final String Q_APP_STATUS = "app.status.queue";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean public Queue jobCreatedQueue() { return QueueBuilder.durable(Q_JOB_CREATED).build(); }
    @Bean public Queue jobAppliedQueue() { return QueueBuilder.durable(Q_JOB_APPLIED).build(); }
    @Bean public Queue jobClosedQueue() { return QueueBuilder.durable(Q_JOB_CLOSED).build(); }
    @Bean public Queue appStatusQueue() { return QueueBuilder.durable(Q_APP_STATUS).build(); }

    @Bean public Binding jobCreatedBinding(Queue jobCreatedQueue, TopicExchange exchange) { return BindingBuilder.bind(jobCreatedQueue).to(exchange).with("job.created"); }
    @Bean public Binding jobAppliedBinding(Queue jobAppliedQueue, TopicExchange exchange) { return BindingBuilder.bind(jobAppliedQueue).to(exchange).with("job.applied"); }
    @Bean public Binding jobClosedBinding(Queue jobClosedQueue, TopicExchange exchange) { return BindingBuilder.bind(jobClosedQueue).to(exchange).with("job.closed"); }
    @Bean public Binding appStatusBinding(Queue appStatusQueue, TopicExchange exchange) { return BindingBuilder.bind(appStatusQueue).to(exchange).with("app.status.changed"); }

    @Bean public MessageConverter jsonMessageConverter() { return new Jackson2JsonMessageConverter(); }
    @Bean public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
