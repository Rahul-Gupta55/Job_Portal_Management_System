package com.jobportal.notificationservice.controller;

import com.jobportal.commonsecurity.security.JwtUserPrincipal;
import com.jobportal.notificationservice.dto.ApiResponse;
import com.jobportal.notificationservice.dto.NotificationResponse;
import com.jobportal.notificationservice.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    @Test
    void getByUserDelegatesUsingPrincipalIdentity() {
        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "user@example.com", "JOB_SEEKER");
        when(notificationService.getByUser(5L, 5L, "JOB_SEEKER")).thenReturn(List.of(NotificationResponse.builder().id(1L).build()));

        ResponseEntity<ApiResponse<List<NotificationResponse>>> response = notificationController.getByUser(5L, principal);

        assertThat(response.getBody().getMessage()).isEqualTo("Notifications fetched successfully");
        assertThat(response.getBody().getData()).hasSize(1);
    }

    @Test
    void markReadDelegatesUsingPrincipalIdentity() {
        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "user@example.com", "JOB_SEEKER");
        when(notificationService.markRead(1L, 5L, "JOB_SEEKER")).thenReturn(NotificationResponse.builder().id(1L).read(true).build());

        ResponseEntity<ApiResponse<NotificationResponse>> response = notificationController.markRead(1L, principal);

        assertThat(response.getBody().getMessage()).isEqualTo("Notification marked as read");
        assertThat(response.getBody().getData().isRead()).isTrue();
    }

    @Test
    void markAllReadReturnsSuccessMessage() {
        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "user@example.com", "JOB_SEEKER");

        ResponseEntity<ApiResponse<Void>> response = notificationController.markAllRead(5L, principal);

        assertThat(response.getBody().getMessage()).isEqualTo("All notifications marked as read");
        verify(notificationService).markAllRead(5L, 5L, "JOB_SEEKER");
    }

    @Test
    void deleteReturnsSuccessMessage() {
        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "user@example.com", "JOB_SEEKER");

        ResponseEntity<ApiResponse<Void>> response = notificationController.delete(1L, principal);

        assertThat(response.getBody().getMessage()).isEqualTo("Notification deleted successfully");
        verify(notificationService).delete(1L, 5L, "JOB_SEEKER");
    }
}
