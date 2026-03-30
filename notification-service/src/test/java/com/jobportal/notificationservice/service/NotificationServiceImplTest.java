package com.jobportal.notificationservice.service;

import com.jobportal.notificationservice.dto.NotificationResponse;
import com.jobportal.notificationservice.entity.Notification;
import com.jobportal.notificationservice.entity.NotificationStatus;
import com.jobportal.notificationservice.entity.NotificationType;
import com.jobportal.notificationservice.event.AppStatusChangedEvent;
import com.jobportal.notificationservice.event.JobAppliedEvent;
import com.jobportal.notificationservice.event.JobCreatedEvent;
import com.jobportal.notificationservice.exception.UnauthorizedActionException;
import com.jobportal.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    void getByUserReturnsOnlyOwnersNotifications() {
        Notification notification = Notification.builder()
                .id(1L)
                .recipientId(5L)
                .type(NotificationType.EMAIL)
                .subject("Subject")
                .body("Body")
                .status(NotificationStatus.SENT)
                .build();

        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(5L))
                .thenReturn(List.of(notification));

        List<NotificationResponse> result = notificationService.getByUser(5L, 5L, "JOB_SEEKER");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSubject()).isEqualTo("Subject");
    }

    @Test
    void getByUserRejectsDifferentUserWithoutAdminRole() {
        assertThatThrownBy(() -> notificationService.getByUser(5L, 6L, "JOB_SEEKER"))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    void markReadUpdatesReadFlags() {
        Notification notification = Notification.builder()
                .id(1L)
                .recipientId(5L)
                .type(NotificationType.EMAIL)
                .body("Body")
                .status(NotificationStatus.SENT)
                .read(false)
                .build();

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = notificationService.markRead(1L, 5L, "JOB_SEEKER");

        assertThat(response.isRead()).isTrue();
        assertThat(notification.getReadAt()).isNotNull();
    }

    @Test
    void markAllReadMarksUnreadNotifications() {
        Notification first = Notification.builder()
                .id(1L)
                .recipientId(5L)
                .type(NotificationType.EMAIL)
                .body("One")
                .status(NotificationStatus.SENT)
                .read(false)
                .build();

        Notification second = Notification.builder()
                .id(2L)
                .recipientId(5L)
                .type(NotificationType.EMAIL)
                .body("Two")
                .status(NotificationStatus.SENT)
                .read(false)
                .build();

        when(notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(5L))
                .thenReturn(List.of(first, second));

        notificationService.markAllRead(5L, 5L, "JOB_SEEKER");

        assertThat(first.isRead()).isTrue();
        assertThat(second.isRead()).isTrue();
        assertThat(first.getReadAt()).isNotNull();
        assertThat(second.getReadAt()).isNotNull();
    }

    @Test
    void deleteRejectsDifferentUserWithoutAdminRole() {
        Notification notification = Notification.builder()
                .id(1L)
                .recipientId(5L)
                .type(NotificationType.EMAIL)
                .body("Body")
                .status(NotificationStatus.SENT)
                .build();

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.delete(1L, 99L, "JOB_SEEKER"))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    void handleJobCreatedCreatesRecruiterNotification() {
        JobCreatedEvent event = JobCreatedEvent.builder()
                .jobId(10L)
                .title("Java Developer")
                .company("Acme")
                .recruiterId(7L)
                .build();

        notificationService.handleJobCreated(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getRecipientId()).isEqualTo(7L);
        assertThat(captor.getValue().getSubject()).contains("Job created");
    }

    @Test
    void handleJobAppliedCreatesTwoNotifications() {
        JobAppliedEvent event = JobAppliedEvent.builder()
                .applicationId(9L)
                .jobId(10L)
                .candidateId(5L)
                .recruiterId(7L)
                .build();

        notificationService.handleJobApplied(event);

        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void handleStatusChangedCreatesCandidateNotification() {
        AppStatusChangedEvent event = AppStatusChangedEvent.builder()
                .applicationId(9L)
                .jobId(10L)
                .candidateId(5L)
                .recruiterId(7L)
                .newStatus("SHORTLISTED")
                .build();

        notificationService.handleStatusChanged(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getBody()).contains("SHORTLISTED");
        assertThat(captor.getValue().getRecipientId()).isEqualTo(5L);
    }
}