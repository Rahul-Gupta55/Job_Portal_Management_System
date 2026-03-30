package com.jobportal.userservice.service;

import com.jobportal.userservice.dto.*;
import com.jobportal.userservice.entity.Role;
import com.jobportal.commonsecurity.security.JwtUserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(RefreshTokenRequest request);
    UserResponse getUserById(Long id, JwtUserPrincipal principal);
    UserResponse updateUser(Long id, UpdateUserRequest request, JwtUserPrincipal principal);
    void changePassword(Long id, ChangePasswordRequest request, JwtUserPrincipal principal);
    void requestPasswordResetOtp(ForgotPasswordRequest request);
    void resetPasswordWithOtp(ResetPasswordWithOtpRequest request);
    void deactivateUser(Long id);
    InternalUserContactResponse getInternalUserContact(Long id);
    Page<UserResponse> listUsers(Role role, Pageable pageable);
    UserProfileResponse getProfile(Long userId, JwtUserPrincipal principal);
    UserProfileResponse upsertProfile(Long userId, UserProfileRequest request, JwtUserPrincipal principal);
}