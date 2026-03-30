package com.jobportal.notificationservice.service;

import com.jobportal.notificationservice.dto.InternalEmailRequest;
import com.jobportal.notificationservice.entity.Notification;
import com.jobportal.notificationservice.entity.NotificationStatus;
import com.jobportal.notificationservice.entity.NotificationType;
import com.jobportal.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EmailNotificationServiceImpl implements EmailNotificationService {

    private final JavaMailSender javaMailSender;
    private final NotificationRepository notificationRepository;
    private final EmailTemplateService emailTemplateService;

    @Value("${mail.from:${spring.mail.username}}")
    private String fromAddress;

    @Override
    public void sendEmail(InternalEmailRequest request) {
        Notification notification = Notification.builder()
                .recipientId(request.getRecipientId())
                .type(NotificationType.EMAIL)
                .subject(request.getSubject())
                .body(request.getBody())
                .status(NotificationStatus.PENDING)
                .eventType(request.getEventType())
                .referenceId(request.getReferenceId())
                .retryCount(0)
                .read(false)
                .build();

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(request.getTo());
            message.setSubject(request.getSubject());
            message.setText(emailTemplateService.buildProfessionalPlainTextEmail(request.getSubject(), request.getBody()));
            javaMailSender.send(message);

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            log.info("event=email_sent, service=notification-service recipientId={} to={} eventType={}",
                    request.getRecipientId(), request.getTo(), request.getEventType());
        } catch (Exception ex) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setRetryCount(notification.getRetryCount() + 1);
            log.error("event=email_send_failed, service=notification-service recipientId={} to={} eventType={}",
                    request.getRecipientId(), request.getTo(), request.getEventType(), ex);
            if (ex instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Failed to send email", ex);
        } finally {
            notificationRepository.save(notification);
        }
    }
}