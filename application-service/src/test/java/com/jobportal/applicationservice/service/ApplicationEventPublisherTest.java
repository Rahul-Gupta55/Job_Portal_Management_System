package com.jobportal.applicationservice.service;

import com.jobportal.applicationservice.config.RabbitMQConfig;
import com.jobportal.applicationservice.event.AppStatusChangedEvent;
import com.jobportal.applicationservice.event.JobAppliedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.AmqpTemplate;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ApplicationEventPublisherTest {

    @Mock
    private AmqpTemplate amqpTemplate;

    @InjectMocks
    private ApplicationEventPublisher publisher;

    @Test
    void publishJobAppliedSendsExpectedRoutingKey() {
        JobAppliedEvent event = JobAppliedEvent.builder()
                .applicationId(10L)
                .jobId(20L)
                .candidateId(30L)
                .recruiterId(40L)
                .appliedAt(LocalDateTime.now())
                .build();

        publisher.publishJobApplied(event);

        verify(amqpTemplate).convertAndSend(RabbitMQConfig.EXCHANGE, "job.applied", event);
    }

    @Test
    void publishStatusChangedSendsExpectedRoutingKey() {
        AppStatusChangedEvent event = AppStatusChangedEvent.builder()
                .applicationId(11L)
                .jobId(21L)
                .candidateId(31L)
                .recruiterId(41L)
                .newStatus("SHORTLISTED")
                .notes("Strong profile")
                .changedAt(LocalDateTime.now())
                .build();

        publisher.publishStatusChanged(event);

        verify(amqpTemplate).convertAndSend(RabbitMQConfig.EXCHANGE, "app.status.changed", event);
    }
}
