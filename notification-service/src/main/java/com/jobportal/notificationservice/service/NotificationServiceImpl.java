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
import com.jobportal.notificationservice.event.JobClosedEvent;
import com.jobportal.notificationservice.event.JobCreatedEvent;
import com.jobportal.notificationservice.exception.ResourceNotFoundException;
import com.jobportal.notificationservice.exception.UnauthorizedActionException;
import com.jobportal.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final NotificationRepository notificationRepository;
    private final EmailNotificationService emailNotificationService;
    private final UserServiceClient userServiceClient;

    @Value("${app.internal.api-key:jobportal-internal-key}")
    private String internalApiKey;

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
        dispatchEmailNotification(event.getRecruiterId(), "job.created", event.getJobId(), recipient -> new EmailPayload(
                "Job posting published successfully",
                buildJobCreatedBody(recipient, event)
        ));
    }

    @Override
    public void handleJobApplied(JobAppliedEvent event) {
        String candidateName = fetchDisplayName(event.getCandidateId(), "A candidate");

        dispatchEmailNotification(event.getCandidateId(), "job.applied", event.getApplicationId(), recipient -> new EmailPayload(
                "Application submitted successfully",
                buildJobAppliedCandidateBody(recipient, event)
        ));

        dispatchEmailNotification(event.getRecruiterId(), "job.applied", event.getApplicationId(), recipient -> new EmailPayload(
                "New application received",
                buildJobAppliedRecruiterBody(recipient, event, candidateName)
        ));
    }

    @Override
    public void handleJobClosed(JobClosedEvent event) {
        dispatchEmailNotification(event.getRecruiterId(), "job.closed", event.getJobId(), recipient -> new EmailPayload(
                "Job posting closed",
                buildJobClosedBody(recipient, event)
        ));
    }

    @Override
    public void handleStatusChanged(AppStatusChangedEvent event) {
        dispatchEmailNotification(event.getCandidateId(), "app.status.changed", event.getApplicationId(), recipient -> new EmailPayload(
                "Application status updated",
                buildStatusChangedBody(recipient, event)
        ));
    }

    private void dispatchEmailNotification(Long recipientId,
                                           String eventType,
                                           Long referenceId,
                                           Function<UserContactResponse, EmailPayload> payloadFactory) {
        try {
            UserContactResponse recipient = fetchRecipient(recipientId);
            if (recipient == null) {
                saveFailedNotification(recipientId, "Notification could not be delivered",
                        "Recipient lookup failed for event type " + eventType + ".", eventType, referenceId, 1);
                log.error("event=notification_email_skipped, service=notification-service reason=recipient_lookup_failed recipientId={} eventType={} referenceId={}",
                        recipientId, eventType, referenceId);
                return;
            }

            if (!recipient.isActive() || recipient.getEmail() == null || recipient.getEmail().isBlank()) {
                saveFailedNotification(recipientId, "Notification could not be delivered",
                        "Recipient email is unavailable or the account is inactive.", eventType, referenceId, 1);
                log.warn("event=notification_email_skipped, service=notification-service reason=recipient_unavailable recipientId={} active={} eventType={} referenceId={}",
                        recipientId, recipient.isActive(), eventType, referenceId);
                return;
            }

            EmailPayload payload = payloadFactory.apply(recipient);
            emailNotificationService.sendEmail(InternalEmailRequest.builder()
                    .recipientId(recipientId)
                    .to(recipient.getEmail())
                    .subject(payload.subject())
                    .body(payload.body())
                    .eventType(eventType)
                    .referenceId(referenceId)
                    .build());
        } catch (Exception ex) {
            log.error("event=notification_email_dispatch_failed, service=notification-service recipientId={} eventType={} referenceId={}",
                    recipientId, eventType, referenceId, ex);
        }
    }

    private UserContactResponse fetchRecipient(Long recipientId) {
        ApiResponse<UserContactResponse> response = userServiceClient.getInternalUserContact(internalApiKey, recipientId);
        return response == null ? null : response.getData();
    }

    private String fetchDisplayName(Long userId, String fallback) {
        try {
            UserContactResponse user = fetchRecipient(userId);
            if (user == null || user.getName() == null || user.getName().isBlank()) {
                return fallback;
            }
            return user.getName();
        } catch (Exception ex) {
            log.warn("event=notification_display_name_lookup_failed, service=notification-service userId={}", userId, ex);
            return fallback;
        }
    }

    private String buildJobCreatedBody(UserContactResponse recipient, JobCreatedEvent event) {
        return "Hello " + displayName(recipient.getName()) + ",\n\n"
                + "Your job posting has been published successfully on Job Portal.\n\n"
                + "Position: " + safeValue(event.getTitle(), "New job posting") + "\n"
                + "Company: " + safeValue(event.getCompany(), "Your organization") + "\n"
                + optionalDateLine("Published on", event.getCreatedAt())
                + "You can now start reviewing applications from your recruiter dashboard.\n\n"
                + "Regards,\nJob Portal Team";
    }

    private String buildJobAppliedCandidateBody(UserContactResponse recipient, JobAppliedEvent event) {
        return "Hello " + displayName(recipient.getName()) + ",\n\n"
                + "Your application has been submitted successfully.\n\n"
                + "Position: " + safeValue(event.getJobTitle(), "Selected position") + "\n"
                + optionalDateLine("Submitted on", event.getAppliedAt())
                + "We will notify you as soon as the recruiter updates the status of your application.\n\n"
                + "Regards,\nJob Portal Team";
    }

    private String buildJobAppliedRecruiterBody(UserContactResponse recipient, JobAppliedEvent event, String candidateName) {
        return "Hello " + displayName(recipient.getName()) + ",\n\n"
                + "A new application has been received for your job posting.\n\n"
                + "Position: " + safeValue(event.getJobTitle(), "Your active job posting") + "\n"
                + "Applicant: " + safeValue(candidateName, "A candidate") + "\n"
                + optionalDateLine("Received on", event.getAppliedAt())
                + "Please review the application from your recruiter dashboard.\n\n"
                + "Regards,\nJob Portal Team";
    }

    private String buildJobClosedBody(UserContactResponse recipient, JobClosedEvent event) {
        return "Hello " + displayName(recipient.getName()) + ",\n\n"
                + "Your job posting has been closed successfully.\n\n"
                + "Position: " + safeValue(event.getTitle(), "Selected position") + "\n"
                + "Company: " + safeValue(event.getCompany(), "Your organization") + "\n"
                + optionalDateLine("Closed on", event.getClosedAt())
                + "You can reopen the posting later if you want to accept more applications.\n\n"
                + "Regards,\nJob Portal Team";
    }

    private String buildStatusChangedBody(UserContactResponse recipient, AppStatusChangedEvent event) {
        StringBuilder body = new StringBuilder()
                .append("Hello ").append(displayName(recipient.getName())).append(",\n\n")
                .append("The status of your application has been updated.\n\n")
                .append("Position: ").append(safeValue(event.getJobTitle(), "Applied position")).append("\n")
                .append("Current status: ").append(formatStatus(event.getNewStatus())).append("\n")
                .append(optionalDateLine("Updated on", event.getChangedAt()));

        if (event.getNotes() != null && !event.getNotes().isBlank()) {
            body.append("Recruiter note: ").append(event.getNotes().trim()).append("\n");
        }

        body.append("\nPlease log in to Job Portal to review the latest application details.\n\n")
                .append("Regards,\nJob Portal Team");

        return body.toString();
    }

    private String optionalDateLine(String label, LocalDateTime value) {
        return value == null ? "" : label + ": " + DATE_TIME_FORMATTER.format(value) + "\n";
    }

    private String displayName(String name) {
        return safeValue(name, "User");
    }

    private String safeValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String formatStatus(String status) {
        if (status == null || status.isBlank()) {
            return "Updated";
        }
        String[] parts = status.toLowerCase(Locale.ROOT).split("_");
        StringBuilder formatted = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!formatted.isEmpty()) {
                formatted.append(' ');
            }
            formatted.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                formatted.append(part.substring(1));
            }
        }
        return formatted.isEmpty() ? "Updated" : formatted.toString();
    }

    private void saveFailedNotification(Long recipientId, String subject, String body, String eventType, Long referenceId, int retryCount) {
        Notification notification = Notification.builder()
                .recipientId(recipientId)
                .type(NotificationType.EMAIL)
                .subject(subject)
                .body(body)
                .status(NotificationStatus.FAILED)
                .eventType(eventType)
                .referenceId(referenceId)
                .retryCount(retryCount)
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

    private record EmailPayload(String subject, String body) {
    }
}