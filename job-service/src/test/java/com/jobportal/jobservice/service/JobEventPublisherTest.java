package com.jobportal.jobservice.service;

import com.jobportal.jobservice.config.RabbitMQConfig;
import com.jobportal.jobservice.event.JobClosedEvent;
import com.jobportal.jobservice.event.JobCreatedEvent;
import com.jobportal.jobservice.event.JobDeletedEvent;
import com.jobportal.jobservice.event.JobUpsertedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.AmqpTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JobEventPublisherTest {

    @Mock
    private AmqpTemplate amqpTemplate;

    @InjectMocks
    private JobEventPublisher publisher;

    @Test
    void publishJobCreatedSendsExpectedRoutingKey() {
        JobCreatedEvent event = JobCreatedEvent.builder()
                .jobId(10L)
                .title("Java Developer")
                .company("Acme")
                .recruiterId(99L)
                .createdAt(LocalDateTime.now())
                .build();

        publisher.publishJobCreated(event);

        verify(amqpTemplate).convertAndSend(RabbitMQConfig.EXCHANGE, "job.created", event);
    }

    @Test
    void publishJobClosedSendsExpectedRoutingKey() {
        JobClosedEvent event = JobClosedEvent.builder()
                .jobId(11L)
                .recruiterId(99L)
                .closedAt(LocalDateTime.now())
                .build();

        publisher.publishJobClosed(event);

        verify(amqpTemplate).convertAndSend(RabbitMQConfig.EXCHANGE, "job.closed", event);
    }

    @Test
    void publishJobUpsertedSendsExpectedRoutingKey() {
        JobUpsertedEvent event = JobUpsertedEvent.builder()
                .jobId(12L)
                .title("Backend Engineer")
                .company("Acme")
                .location("Hyderabad")
                .salaryRange("10-12 LPA")
                .jobType("FULL_TIME")
                .status("OPEN")
                .recruiterId(99L)
                .experienceMin(2)
                .experienceMax(4)
                .deadline(LocalDate.now().plusDays(30))
                .createdAt(LocalDateTime.now())
                .build();

        publisher.publishJobUpserted(event);

        verify(amqpTemplate).convertAndSend(RabbitMQConfig.EXCHANGE, "job.upserted", event);
    }

    @Test
    void publishJobDeletedSendsExpectedRoutingKey() {
        JobDeletedEvent event = JobDeletedEvent.builder().jobId(13L).build();

        publisher.publishJobDeleted(event);

        verify(amqpTemplate).convertAndSend(RabbitMQConfig.EXCHANGE, "job.deleted", event);
    }
}
