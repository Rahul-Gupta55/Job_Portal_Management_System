package com.jobportal.jobservice.service;

import com.jobportal.jobservice.config.RabbitMQConfig;
import com.jobportal.jobservice.event.JobClosedEvent;
import com.jobportal.jobservice.event.JobCreatedEvent;
import com.jobportal.jobservice.event.JobDeletedEvent;
import com.jobportal.jobservice.event.JobUpsertedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobEventPublisher {

    private final AmqpTemplate amqpTemplate;

    public void publishJobCreated(JobCreatedEvent event) {
        amqpTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, "job.created", event);
        log.info("event=message_published service=job-service exchange={} routingKey=job.created jobId={} recruiterId={}",
                RabbitMQConfig.EXCHANGE, event.getJobId(), event.getRecruiterId());
    }

    public void publishJobClosed(JobClosedEvent event) {
        amqpTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, "job.closed", event);
        log.info("event=message_published service=job-service exchange={} routingKey=job.closed jobId={} recruiterId={}",
                RabbitMQConfig.EXCHANGE, event.getJobId(), event.getRecruiterId());
    }

    public void publishJobUpserted(JobUpsertedEvent event) {
        amqpTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, "job.upserted", event);
        log.info("event=message_published service=job-service exchange={} routingKey=job.upserted jobId={} recruiterId={} status={}",
                RabbitMQConfig.EXCHANGE, event.getJobId(), event.getRecruiterId(), event.getStatus());
    }

    public void publishJobDeleted(JobDeletedEvent event) {
        amqpTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, "job.deleted", event);
        log.info("event=message_published service=job-service exchange={} routingKey=job.deleted jobId={}",
                RabbitMQConfig.EXCHANGE, event.getJobId());
    }
}
