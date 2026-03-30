package com.jobportal.notificationservice.service;

import com.jobportal.notificationservice.config.RabbitMQConfig;
import com.jobportal.notificationservice.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.Q_JOB_CREATED)
    public void onJobCreated(JobCreatedEvent event) {
        log.info("Received job.created event for jobId={}", event.getJobId());
        notificationService.handleJobCreated(event);
    }

    @RabbitListener(queues = RabbitMQConfig.Q_JOB_APPLIED)
    public void onJobApplied(JobAppliedEvent event) {
        log.info("Received job.applied event for applicationId={}", event.getApplicationId());
        notificationService.handleJobApplied(event);
    }

    @RabbitListener(queues = RabbitMQConfig.Q_JOB_CLOSED)
    public void onJobClosed(JobClosedEvent event) {
        log.info("Received job.closed event for jobId={}", event.getJobId());
        notificationService.handleJobClosed(event);
    }

    @RabbitListener(queues = RabbitMQConfig.Q_APP_STATUS)
    public void onStatusChanged(AppStatusChangedEvent event) {
        log.info("Received app.status.changed event for applicationId={}", event.getApplicationId());
        notificationService.handleStatusChanged(event);
    }
}
