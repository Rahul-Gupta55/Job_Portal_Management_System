package com.jobportal.notificationservice.service;

import com.jobportal.notificationservice.client.UserServiceClient;
import com.jobportal.notificationservice.dto.ApiResponse;
import com.jobportal.notificationservice.dto.InternalEmailRequest;
import com.jobportal.notificationservice.dto.NotificationResponse;
import com.jobportal.notificationservice.dto.UserContactResponse;
import com.jobportal.notificationservice.entity.Notification;
import com.jobportal.notificationservice.entity.NotificationStatus;
import com.jobportal.notificationservice.entity.NotificationType;
import com.jobportal.notificationservice.event.AppStatusChangedEvent;
import com.jobportal.notificationservice.event.JobAppliedEvent;
import com.jobportal.notificationservice.event.JobCreatedEvent;
import com.jobportal.notificationservice.exception.UnauthorizedActionException;
import com.jobportal.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private EmailNotificationService emailNotificationService;
    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationService, "internalApiKey", "jobportal-internal-key");
    }

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
    void handleJobCreatedSendsRealEmailToRecruiter() {
        JobCreatedEvent event = JobCreatedEvent.builder()
                .jobId(10L)
                .title("Java Developer")
                .company("Acme")
                .recruiterId(7L)
                .build();
        when(userServiceClient.getInternalUserContact("jobportal-internal-key", 7L))
                .thenReturn(ApiResponse.of("ok", UserContactResponse.builder()
                        .id(7L)
                        .name("Recruiter")
                        .email("recruiter@example.com")
                        .role("RECRUITER")
                        .active(true)
                        .build()));

        notificationService.handleJobCreated(event);

        ArgumentCaptor<InternalEmailRequest> captor = ArgumentCaptor.forClass(InternalEmailRequest.class);
        verify(emailNotificationService).sendEmail(captor.capture());
        assertThat(captor.getValue().getRecipientId()).isEqualTo(7L);
        assertThat(captor.getValue().getTo()).isEqualTo("recruiter@example.com");
        assertThat(captor.getValue().getSubject()).isEqualTo("Job posting published successfully");
        assertThat(captor.getValue().getBody()).contains("Hello Recruiter");
        assertThat(captor.getValue().getBody()).contains("Position: Java Developer");
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void handleJobAppliedSendsTwoEmails() {
        JobAppliedEvent event = JobAppliedEvent.builder()
                .applicationId(9L)
                .jobId(10L)
                .candidateId(5L)
                .recruiterId(7L)
                .jobTitle("Java Developer")
                .build();
        when(userServiceClient.getInternalUserContact(eq("jobportal-internal-key"), any(Long.class)))
                .thenAnswer(invocation -> {
                    Long id = invocation.getArgument(1);
                    return ApiResponse.of("ok", UserContactResponse.builder()
                            .id(id)
                            .name("User" + id)
                            .email("user" + id + "@example.com")
                            .role(id.equals(7L) ? "RECRUITER" : "JOB_SEEKER")
                            .active(true)
                            .build());
                });

        notificationService.handleJobApplied(event);

        ArgumentCaptor<InternalEmailRequest> captor = ArgumentCaptor.forClass(InternalEmailRequest.class);
        verify(emailNotificationService, times(2)).sendEmail(captor.capture());
        assertThat(captor.getAllValues()).extracting(InternalEmailRequest::getSubject)
                .containsExactlyInAnyOrder("Application submitted successfully", "New application received");
        assertThat(captor.getAllValues().stream()
                .map(InternalEmailRequest::getBody)
                .anyMatch(body -> body.contains("Position: Java Developer")))
                .isTrue();
    }

    @Test
    void handleStatusChangedCreatesFailedNotificationWhenRecipientIsInactive() {
        AppStatusChangedEvent event = AppStatusChangedEvent.builder()
                .applicationId(9L)
                .jobId(10L)
                .candidateId(5L)
                .recruiterId(7L)
                .jobTitle("Java Developer")
                .newStatus("SHORTLISTED")
                .build();
        when(userServiceClient.getInternalUserContact("jobportal-internal-key", 5L))
                .thenReturn(ApiResponse.of("ok", UserContactResponse.builder()
                        .id(5L)
                        .name("Candidate")
                        .email("candidate@example.com")
                        .role("JOB_SEEKER")
                        .active(false)
                        .build()));

        notificationService.handleStatusChanged(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(captor.getValue().getRecipientId()).isEqualTo(5L);
        verify(emailNotificationService, never()).sendEmail(any());
    }
}