package com.jobportal.notificationservice.controller;

import com.jobportal.notificationservice.dto.ApiResponse;
import com.jobportal.notificationservice.dto.InternalEmailRequest;
import com.jobportal.notificationservice.service.EmailNotificationService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping("/api/internal/notifications")
@RequiredArgsConstructor
@Hidden
public class InternalNotificationController {

    private final EmailNotificationService emailNotificationService;

    @Value("${app.internal.api-key:jobportal-internal-key}")
    private String internalApiKey;

    @PostMapping("/email")
    public ResponseEntity<ApiResponse<Void>> sendEmail(@RequestHeader("X-Internal-Api-Key") String requestApiKey,
                                                       @Valid @RequestBody InternalEmailRequest request) {
        if (!internalApiKey.equals(requestApiKey)) {
            throw new ResponseStatusException(FORBIDDEN, "Invalid internal API key");
        }

        emailNotificationService.sendEmail(request);
        return ResponseEntity.ok(ApiResponse.of("Email sent successfully", null));
    }
}