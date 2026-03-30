package com.jobportal.notificationservice.service;

import com.jobportal.notificationservice.dto.InternalEmailRequest;
import com.jobportal.notificationservice.entity.Notification;
import com.jobportal.notificationservice.entity.NotificationStatus;
import com.jobportal.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceImplTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmailTemplateService emailTemplateService;

    @InjectMocks
    private EmailNotificationServiceImpl emailNotificationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailNotificationService, "fromAddress", "yagnamadhavkolasani2004@gmail.com");
    }

    @Test
    void sendEmailSendsProfessionalPlainTextMailAndStoresSentNotification() {
        InternalEmailRequest request = InternalEmailRequest.builder()
                .recipientId(5L)
                .to("alice@example.com")
                .subject("OTP")
                .body("Your OTP is 123456")
                .eventType("password.reset.otp")
                .referenceId(9L)
                .build();
        when(emailTemplateService.buildProfessionalPlainTextEmail("OTP", "Your OTP is 123456"))
                .thenReturn("formatted-body");

        emailNotificationService.sendEmail(request);

        verify(emailTemplateService).buildProfessionalPlainTextEmail("OTP", "Your OTP is 123456");
        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(mailCaptor.capture());
        assertThat(mailCaptor.getValue().getSubject()).isEqualTo("OTP");
        assertThat(mailCaptor.getValue().getTo()).containsExactly("alice@example.com");
        assertThat(mailCaptor.getValue().getText()).isEqualTo("formatted-body");

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notificationCaptor.getValue().getSentAt()).isNotNull();
    }

    @Test
    void sendEmailStoresFailedNotificationWhenMailSenderThrows() {
        InternalEmailRequest request = InternalEmailRequest.builder()
                .recipientId(5L)
                .to("alice@example.com")
                .subject("OTP")
                .body("Your OTP is 123456")
                .eventType("password.reset.otp")
                .referenceId(9L)
                .build();
        when(emailTemplateService.buildProfessionalPlainTextEmail("OTP", "Your OTP is 123456"))
                .thenReturn("formatted-body");
        doThrow(new MailSendException("smtp down")).when(javaMailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> emailNotificationService.sendEmail(request))
                .isInstanceOf(MailSendException.class);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(notificationCaptor.getValue().getRetryCount()).isEqualTo(1);
    }
}