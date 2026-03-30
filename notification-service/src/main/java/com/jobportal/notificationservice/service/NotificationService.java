package com.jobportal.notificationservice.service;

import com.jobportal.notificationservice.dto.NotificationResponse;
import com.jobportal.notificationservice.entity.Notification;
import com.jobportal.notificationservice.event.*;

import java.util.List;

public interface NotificationService {
    List<NotificationResponse> getByUser(Long userId, Long requesterId, String role);
    NotificationResponse markRead(Long id, Long requesterId, String role);
    void markAllRead(Long userId, Long requesterId, String role);
    void delete(Long id, Long requesterId, String role);

    void handleJobCreated(JobCreatedEvent event);
    void handleJobApplied(JobAppliedEvent event);
    void handleJobClosed(JobClosedEvent event);
    void handleStatusChanged(AppStatusChangedEvent event);
}
