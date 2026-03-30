package com.jobportal.notificationservice.service;

import com.jobportal.notificationservice.dto.NotificationResponse;
import com.jobportal.notificationservice.entity.*;
import com.jobportal.notificationservice.event.*;
import com.jobportal.notificationservice.exception.ResourceNotFoundException;
import com.jobportal.notificationservice.exception.UnauthorizedActionException;
import com.jobportal.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getByUser(Long userId, Long requesterId, String role) {
        ensureOwnerOrAdmin(userId, requesterId, role);
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse).toList();
    }

    @Override
    public NotificationResponse markRead(Long id, Long requesterId, String role) {
        Notification notification = findById(id);
        ensureOwnerOrAdmin(notification.getRecipientId(), requesterId, role);
        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now());
        return toResponse(notificationRepository.save(notification));
    }

    @Override
    public void markAllRead(Long userId, Long requesterId, String role) {
        ensureOwnerOrAdmin(userId, requesterId, role);
        notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(userId).forEach(notification -> {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
        });
    }

    @Override
    public void delete(Long id, Long requesterId, String role) {
        Notification notification = findById(id);
        ensureOwnerOrAdmin(notification.getRecipientId(), requesterId, role);
        notificationRepository.delete(notification);
    }

    @Override
    public void handleJobCreated(JobCreatedEvent event) {
        saveNotification(event.getRecruiterId(),
                "Job created successfully",
                "Your job '" + event.getTitle() + "' at " + event.getCompany() + " was created.",
                "job.created",
                event.getJobId());
    }

    @Override
    public void handleJobApplied(JobAppliedEvent event) {
        saveNotification(event.getCandidateId(),
                "Application submitted",
                "Your application for job ID " + event.getJobId() + " has been submitted successfully.",
                "job.applied",
                event.getApplicationId());

        saveNotification(event.getRecruiterId(),
                "New application received",
                "A candidate has applied to your job ID " + event.getJobId() + ".",
                "job.applied",
                event.getApplicationId());
    }

    @Override
    public void handleJobClosed(JobClosedEvent event) {
        saveNotification(event.getRecruiterId(),
                "Job closed",
                "Your job ID " + event.getJobId() + " has been closed.",
                "job.closed",
                event.getJobId());
    }

    @Override
    public void handleStatusChanged(AppStatusChangedEvent event) {
        saveNotification(event.getCandidateId(),
                "Application status updated",
                "Your application status has been changed to " + event.getNewStatus() + ".",
                "app.status.changed",
                event.getApplicationId());
    }

    private void saveNotification(Long recipientId, String subject, String body, String eventType, Long referenceId) {
        Notification notification = Notification.builder()
                .recipientId(recipientId)
                .type(NotificationType.EMAIL)
                .subject(subject)
                .body(body)
                .status(NotificationStatus.SENT)
                .eventType(eventType)
                .referenceId(referenceId)
                .retryCount(0)
                .sentAt(LocalDateTime.now())
                .read(false)
                .build();

        notificationRepository.save(notification);
    }

    private Notification findById(Long id) {
        return notificationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Notification", id));
    }

    private void ensureOwnerOrAdmin(Long ownerId, Long requesterId, String role) {
        if (!ownerId.equals(requesterId) && !"ADMIN".equals(role)) {
            throw new UnauthorizedActionException("You can access only your own notifications");
        }
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .recipientId(notification.getRecipientId())
                .type(notification.getType().name())
                .subject(notification.getSubject())
                .body(notification.getBody())
                .status(notification.getStatus().name())
                .eventType(notification.getEventType())
                .referenceId(notification.getReferenceId())
                .read(notification.isRead())
                .sentAt(notification.getSentAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
