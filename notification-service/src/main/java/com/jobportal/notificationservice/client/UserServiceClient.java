package com.jobportal.notificationservice.client;

import com.jobportal.notificationservice.dto.ApiResponse;
import com.jobportal.notificationservice.dto.UserContactResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/api/internal/users/{id}/contact")
    ApiResponse<UserContactResponse> getInternalUserContact(@RequestHeader("X-Internal-Api-Key") String internalApiKey,
                                                            @PathVariable("id") Long id);
}