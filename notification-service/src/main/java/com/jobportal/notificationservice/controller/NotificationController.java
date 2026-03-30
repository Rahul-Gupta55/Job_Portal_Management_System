package com.jobportal.notificationservice.controller;

import com.jobportal.commonsecurity.security.JwtUserPrincipal;
import com.jobportal.notificationservice.dto.ApiResponse;
import com.jobportal.notificationservice.dto.NotificationResponse;
import com.jobportal.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification Service", description = "Simple notification APIs")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get notifications for a user")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getByUser(@PathVariable Long userId,
                                                                             @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.of("Notifications fetched successfully", notificationService.getByUser(userId, principal.userId(), principal.role())));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markRead(@PathVariable Long id,
                                                                      @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.of("Notification marked as read", notificationService.markRead(id, principal.userId(), principal.role())));
    }

    @PutMapping("/user/{userId}/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<ApiResponse<Void>> markAllRead(@PathVariable Long userId,
                                                         @AuthenticationPrincipal JwtUserPrincipal principal) {
        notificationService.markAllRead(userId, principal.userId(), principal.role());
        return ResponseEntity.ok(ApiResponse.of("All notifications marked as read", null));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete notification")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                    @AuthenticationPrincipal JwtUserPrincipal principal) {
        notificationService.delete(id, principal.userId(), principal.role());
        return ResponseEntity.ok(ApiResponse.of("Notification deleted successfully", null));
    }
}
