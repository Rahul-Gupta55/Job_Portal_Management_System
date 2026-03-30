package com.jobportal.userservice.client;

import com.jobportal.userservice.dto.ApiResponse;
import com.jobportal.userservice.dto.InternalEmailRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "notification-service")
public interface NotificationServiceClient {

    @PostMapping("/api/internal/notifications/email")
    ApiResponse<Void> sendEmail(@RequestHeader("X-Internal-Api-Key") String internalApiKey,
                                @RequestBody InternalEmailRequest request);
}