package com.jobportal.applicationservice.service;

import com.jobportal.applicationservice.config.RabbitMQConfig;
import com.jobportal.applicationservice.event.AppStatusChangedEvent;
import com.jobportal.applicationservice.event.JobAppliedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationEventPublisher {

    private final AmqpTemplate amqpTemplate;

    public void publishJobApplied(JobAppliedEvent event) {
        amqpTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, "job.applied", event);
        log.info("Job applied event published. applicationId={}, jobId={}, candidateId={}",
                event.getApplicationId(), event.getJobId(), event.getCandidateId());
    }

    public void publishStatusChanged(AppStatusChangedEvent event) {
        amqpTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, "app.status.changed", event);
        log.info("Application status changed event published. applicationId={}, status={}",
                event.getApplicationId(), event.getNewStatus());
    }
}