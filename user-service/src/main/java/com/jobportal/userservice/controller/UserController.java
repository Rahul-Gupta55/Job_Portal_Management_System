package com.jobportal.userservice.controller;

import com.jobportal.commonsecurity.security.JwtUserPrincipal;
import com.jobportal.userservice.dto.*;
import com.jobportal.userservice.entity.Role;
import com.jobportal.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Service", description = "Simple auth and profile APIs")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("User registered successfully", userService.register(request)));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and get JWT tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.of("Login successful", userService.login(request)));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh JWT access token using a refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.of("Token refreshed successfully", userService.refreshToken(request)));
    }

    @PostMapping("/forgot-password/request-otp")
    @Operation(summary = "Send OTP to email for password reset")
    public ResponseEntity<ApiResponse<Void>> requestForgotPasswordOtp(@Valid @RequestBody ForgotPasswordRequest request) {
        userService.requestPasswordResetOtp(request);
        return ResponseEntity.ok(ApiResponse.of("If the email is registered, an OTP has been sent", null));
    }

    @PostMapping("/forgot-password/reset")
    @Operation(summary = "Reset password using OTP")
    public ResponseEntity<ApiResponse<Void>> resetPasswordWithOtp(@Valid @RequestBody ResetPasswordWithOtpRequest request) {
        userService.resetPasswordWithOtp(request);
        return ResponseEntity.ok(ApiResponse.of("Password reset successful", null));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user basic details")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable Long id,
                                                             @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.of("User fetched successfully", userService.getUserById(id, principal)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user basic details")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(@PathVariable Long id,
                                                                @Valid @RequestBody UpdateUserRequest request,
                                                                @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.of("User updated successfully", userService.updateUser(id, request, principal)));
    }

    @PatchMapping("/{id}/password")
    @Operation(summary = "Change password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@PathVariable Long id,
                                                            @Valid @RequestBody ChangePasswordRequest request,
                                                            @AuthenticationPrincipal JwtUserPrincipal principal) {
        userService.changePassword(id, request, principal);
        return ResponseEntity.ok(ApiResponse.of("Password changed successfully", null));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate user")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(@PathVariable Long id) {
        userService.deactivateUser(id);
        return ResponseEntity.ok(ApiResponse.of("User deactivated successfully", null));
    }

    @GetMapping
    @Operation(summary = "List users")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> listUsers(@RequestParam(required = false) Role role,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.of("Users fetched successfully", userService.listUsers(role, pageable)));
    }

    @GetMapping("/{id}/profile")
    @Operation(summary = "Get extended profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(@PathVariable Long id,
                                                                       @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.of("Profile fetched successfully", userService.getProfile(id, principal)));
    }

    @PutMapping("/{id}/profile")
    @Operation(summary = "Create or update extended profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(@PathVariable Long id,
                                                                          @RequestBody UserProfileRequest request,
                                                                          @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.of("Profile updated successfully", userService.upsertProfile(id, request, principal)));
    }
}