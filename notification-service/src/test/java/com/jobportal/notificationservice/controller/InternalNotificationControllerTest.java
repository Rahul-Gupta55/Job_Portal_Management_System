package com.jobportal.notificationservice.controller;

import com.jobportal.notificationservice.dto.ApiResponse;
import com.jobportal.notificationservice.dto.InternalEmailRequest;
import com.jobportal.notificationservice.service.EmailNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InternalNotificationControllerTest {

    @Mock
    private EmailNotificationService emailNotificationService;

    @InjectMocks
    private InternalNotificationController internalNotificationController;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(internalNotificationController, "internalApiKey", "jobportal-internal-key");
    }

    @Test
    void sendEmailDelegatesWhenApiKeyMatches() {
        InternalEmailRequest request = InternalEmailRequest.builder()
                .recipientId(5L)
                .to("alice@example.com")
                .subject("OTP")
                .body("Your OTP is 123456")
                .build();

        ResponseEntity<ApiResponse<Void>> response = internalNotificationController.sendEmail("jobportal-internal-key", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getMessage()).isEqualTo("Email sent successfully");
        verify(emailNotificationService).sendEmail(request);
    }

    @Test
    void sendEmailRejectsWrongApiKey() {
        InternalEmailRequest request = InternalEmailRequest.builder()
                .recipientId(5L)
                .to("alice@example.com")
                .subject("OTP")
                .body("Your OTP is 123456")
                .build();

        assertThatThrownBy(() -> internalNotificationController.sendEmail("wrong-key", request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
    }
}