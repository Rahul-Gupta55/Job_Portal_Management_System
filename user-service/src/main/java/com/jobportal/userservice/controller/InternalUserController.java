package com.jobportal.userservice.controller;

import com.jobportal.userservice.dto.ApiResponse;
import com.jobportal.userservice.dto.InternalUserContactResponse;
import com.jobportal.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping("/api/internal/users")
@RequiredArgsConstructor
@Hidden
public class InternalUserController {

    private final UserService userService;

    @Value("${app.internal.api-key:jobportal-internal-key}")
    private String internalApiKey;

    @GetMapping("/{id}/contact")
    public ResponseEntity<ApiResponse<InternalUserContactResponse>> getUserContact(@RequestHeader("X-Internal-Api-Key") String requestApiKey,
                                                                                   @PathVariable Long id) {
        if (!internalApiKey.equals(requestApiKey)) {
            throw new ResponseStatusException(FORBIDDEN, "Invalid internal API key");
        }

        return ResponseEntity.ok(ApiResponse.of("Internal user contact fetched successfully", userService.getInternalUserContact(id)));
    }
}