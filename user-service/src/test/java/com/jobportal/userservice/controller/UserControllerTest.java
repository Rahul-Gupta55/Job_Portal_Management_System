package com.jobportal.userservice.controller;

import com.jobportal.commonsecurity.security.JwtUserPrincipal;
import com.jobportal.userservice.dto.*;
import com.jobportal.userservice.entity.Role;
import com.jobportal.userservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @Test
    void registerReturnsCreatedResponse() {
        RegisterRequest request = RegisterRequest.builder().name("Alice").email("alice@example.com").password("Password1").role(Role.JOB_SEEKER).build();
        UserResponse userResponse = UserResponse.builder().id(1L).email("alice@example.com").role("JOB_SEEKER").build();
        when(userService.register(request)).thenReturn(userResponse);

        ResponseEntity<ApiResponse<UserResponse>> response = userController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getMessage()).isEqualTo("User registered successfully");
        assertThat(response.getBody().getData()).isEqualTo(userResponse);
    }

    @Test
    void loginReturnsAuthPayload() {
        LoginRequest request = LoginRequest.builder().email("alice@example.com").password("Password1").build();
        AuthResponse authResponse = AuthResponse.builder().accessToken("token").refreshToken("refresh").build();
        when(userService.login(request)).thenReturn(authResponse);

        ResponseEntity<ApiResponse<AuthResponse>> response = userController.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getMessage()).isEqualTo("Login successful");
        assertThat(response.getBody().getData()).isEqualTo(authResponse);
    }

    @Test
    void refreshTokenReturnsNewTokens() {
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");
        AuthResponse authResponse = AuthResponse.builder().accessToken("new-token").refreshToken("new-refresh").build();
        when(userService.refreshToken(request)).thenReturn(authResponse);

        ResponseEntity<ApiResponse<AuthResponse>> response = userController.refreshToken(request);

        assertThat(response.getBody().getMessage()).isEqualTo("Token refreshed successfully");
        assertThat(response.getBody().getData()).isEqualTo(authResponse);
    }

    @Test
    void requestForgotPasswordOtpReturnsGenericSuccessMessage() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder().email("alice@example.com").build();

        ResponseEntity<ApiResponse<Void>> response = userController.requestForgotPasswordOtp(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getMessage()).isEqualTo("If the email is registered, an OTP has been sent");
        verify(userService).requestPasswordResetOtp(request);
    }

    @Test
    void resetPasswordWithOtpReturnsSuccessMessage() {
        ResetPasswordWithOtpRequest request = ResetPasswordWithOtpRequest.builder()
                .email("alice@example.com")
                .otp("123456")
                .newPassword("NewPass1")
                .build();

        ResponseEntity<ApiResponse<Void>> response = userController.resetPasswordWithOtp(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getMessage()).isEqualTo("Password reset successful");
        verify(userService).resetPasswordWithOtp(request);
    }

    @Test
    void getUserDelegatesWithPrincipal() {
        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "user@example.com", "JOB_SEEKER");
        UserResponse userResponse = UserResponse.builder().id(5L).build();
        when(userService.getUserById(5L, principal)).thenReturn(userResponse);

        ResponseEntity<ApiResponse<UserResponse>> response = userController.getUser(5L, principal);

        assertThat(response.getBody().getMessage()).isEqualTo("User fetched successfully");
        verify(userService).getUserById(5L, principal);
    }

    @Test
    void updateUserDelegatesWithRequestAndPrincipal() {
        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "user@example.com", "JOB_SEEKER");
        UpdateUserRequest request = UpdateUserRequest.builder().name("Updated").phone("999").build();
        UserResponse userResponse = UserResponse.builder().id(5L).name("Updated").build();
        when(userService.updateUser(5L, request, principal)).thenReturn(userResponse);

        ResponseEntity<ApiResponse<UserResponse>> response = userController.updateUser(5L, request, principal);

        assertThat(response.getBody().getMessage()).isEqualTo("User updated successfully");
        assertThat(response.getBody().getData()).isEqualTo(userResponse);
    }

    @Test
    void changePasswordReturnsSuccessMessage() {
        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "user@example.com", "JOB_SEEKER");
        ChangePasswordRequest request = new ChangePasswordRequest("OldPass1", "NewPass1");

        ResponseEntity<ApiResponse<Void>> response = userController.changePassword(5L, request, principal);

        assertThat(response.getBody().getMessage()).isEqualTo("Password changed successfully");
        assertThat(response.getBody().getData()).isNull();
        verify(userService).changePassword(5L, request, principal);
    }

    @Test
    void deactivateUserReturnsSuccessMessage() {
        ResponseEntity<ApiResponse<Void>> response = userController.deactivateUser(9L);

        assertThat(response.getBody().getMessage()).isEqualTo("User deactivated successfully");
        verify(userService).deactivateUser(9L);
    }

    @Test
    void listUsersBuildsPageableFromRequestParameters() {
        when(userService.listUsers(eq(Role.RECRUITER), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(UserResponse.builder().id(1L).build())));

        ResponseEntity<ApiResponse<org.springframework.data.domain.Page<UserResponse>>> response = userController.listUsers(Role.RECRUITER, 2, 25);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userService).listUsers(eq(Role.RECRUITER), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(25);
        assertThat(response.getBody().getMessage()).isEqualTo("Users fetched successfully");
    }

    @Test
    void getProfileDelegatesWithPrincipal() {
        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "user@example.com", "JOB_SEEKER");
        UserProfileResponse profileResponse = UserProfileResponse.builder().userId(5L).build();
        when(userService.getProfile(5L, principal)).thenReturn(profileResponse);

        ResponseEntity<ApiResponse<UserProfileResponse>> response = userController.getProfile(5L, principal);

        assertThat(response.getBody().getData()).isEqualTo(profileResponse);
        assertThat(response.getBody().getMessage()).isEqualTo("Profile fetched successfully");
    }

    @Test
    void updateProfileDelegatesWithPrincipal() {
        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "user@example.com", "JOB_SEEKER");
        UserProfileRequest request = UserProfileRequest.builder().bio("Bio").skills("Java").build();
        UserProfileResponse profileResponse = UserProfileResponse.builder().userId(5L).bio("Bio").build();
        when(userService.upsertProfile(5L, request, principal)).thenReturn(profileResponse);

        ResponseEntity<ApiResponse<UserProfileResponse>> response = userController.updateProfile(5L, request, principal);

        assertThat(response.getBody().getData()).isEqualTo(profileResponse);
        assertThat(response.getBody().getMessage()).isEqualTo("Profile updated successfully");
    }
}