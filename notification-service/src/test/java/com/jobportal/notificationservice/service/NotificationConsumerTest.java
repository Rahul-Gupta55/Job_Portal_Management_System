package com.jobportal.notificationservice.service;

import com.jobportal.notificationservice.event.AppStatusChangedEvent;
import com.jobportal.notificationservice.event.JobAppliedEvent;
import com.jobportal.notificationservice.event.JobClosedEvent;
import com.jobportal.notificationservice.event.JobCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationConsumer notificationConsumer;

    @Test
    void onJobCreatedDelegatesToService() {
        JobCreatedEvent event = JobCreatedEvent.builder().jobId(1L).recruiterId(2L).build();

        notificationConsumer.onJobCreated(event);

        verify(notificationService).handleJobCreated(event);
    }

    @Test
    void onJobAppliedDelegatesToService() {
        JobAppliedEvent event = JobAppliedEvent.builder().applicationId(10L).jobId(1L).build();

        notificationConsumer.onJobApplied(event);

        verify(notificationService).handleJobApplied(event);
    }

    @Test
    void onJobClosedDelegatesToService() {
        JobClosedEvent event = JobClosedEvent.builder().jobId(1L).recruiterId(2L).build();

        notificationConsumer.onJobClosed(event);

        verify(notificationService).handleJobClosed(event);
    }

    @Test
    void onStatusChangedDelegatesToService() {
        AppStatusChangedEvent event = AppStatusChangedEvent.builder().applicationId(10L).newStatus("SHORTLISTED").build();

        notificationConsumer.onStatusChanged(event);

        verify(notificationService).handleStatusChanged(event);
    }
}