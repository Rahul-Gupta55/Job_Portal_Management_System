package com.jobportal.userservice.service;

import com.jobportal.commonsecurity.security.JwtUserPrincipal;
import com.jobportal.commonsecurity.security.JwtUtil;
import com.jobportal.userservice.client.NotificationServiceClient;
import com.jobportal.userservice.dto.*;
import com.jobportal.userservice.entity.PasswordResetOtp;
import com.jobportal.userservice.entity.Role;
import com.jobportal.userservice.entity.User;
import com.jobportal.userservice.entity.UserProfile;
import com.jobportal.userservice.exception.BadRequestException;
import com.jobportal.userservice.exception.DuplicateEmailException;
import com.jobportal.userservice.exception.ResourceNotFoundException;
import com.jobportal.userservice.exception.ServiceUnavailableException;
import com.jobportal.userservice.exception.UnauthorizedActionException;
import com.jobportal.userservice.repository.PasswordResetOtpRepository;
import com.jobportal.userservice.repository.UserProfileRepository;
import com.jobportal.userservice.repository.UserRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordResetOtpRepository passwordResetOtpRepository;
    private final NotificationServiceClient notificationServiceClient;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Value("${app.password-reset.otp-expiry-minutes:10}")
    private long otpExpiryMinutes;

    @Value("${app.internal.api-key:jobportal-internal-key}")
    private String internalApiKey;

    @Override
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed. Email already exists.");
            throw new DuplicateEmailException(request.getEmail());
        }

        if (request.getRole() == Role.ADMIN) {
            log.warn("Registration failed. Admin registration is not allowed.");
            throw new UnauthorizedActionException("ADMIN registration is not allowed through public API");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .phone(request.getPhone())
                .active(true)
                .build();

        User savedUser = userRepository.save(user);

        UserProfile profile = UserProfile.builder()
                .user(savedUser)
                .experienceYrs(0)
                .build();
        userProfileRepository.save(profile);

        log.info("User registered successfully. userId={}, role={}", savedUser.getId(), savedUser.getRole());
        return toUserResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail().toLowerCase(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User", -1L));

        log.info("User logged in successfully. userId={}, role={}", user.getId(), user.getRole());
        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        try {
            String newAccessToken = jwtUtil.refreshToken(request.getRefreshToken());
            var claims = jwtUtil.extractClaims(request.getRefreshToken());

            Long userId = ((Number) claims.get("userId")).longValue();
            String role = claims.get("role", String.class);

            log.info("Token refreshed successfully. userId={}, role={}", userId, role);

            return AuthResponse.builder()
                    .userId(userId)
                    .email(claims.getSubject())
                    .role(role)
                    .accessToken(newAccessToken)
                    .refreshToken(jwtUtil.generateRefreshToken(userId, claims.getSubject(), role))
                    .tokenType("Bearer")
                    .expiresIn(jwtUtil.getExpirationInSeconds())
                    .refreshExpiresIn(jwtUtil.getRefreshExpirationInSeconds())
                    .build();
        } catch (Exception ex) {
            log.error("Token refresh failed.", ex);
            throw new BadRequestException(ex.getMessage() == null ? "Invalid refresh token" : ex.getMessage());
        }
    }

    @Override
    public void requestPasswordResetOtp(ForgotPasswordRequest request) {
        String email = request.getEmail().toLowerCase();
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            log.warn("Password reset OTP requested for non-existent email={}", email);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        passwordResetOtpRepository.markAllActiveAsUsed(email, now);

        PasswordResetOtp otp = PasswordResetOtp.builder()
                .userId(user.getId())
                .email(email)
                .otpCode(generateOtp())
                .expiresAt(now.plusMinutes(otpExpiryMinutes))
                .used(false)
                .build();
        PasswordResetOtp savedOtp = passwordResetOtpRepository.save(otp);

        String emailBody = buildOtpEmailBody(user.getName(), savedOtp.getOtpCode());

        try {
            notificationServiceClient.sendEmail(internalApiKey, InternalEmailRequest.builder()
                    .recipientId(user.getId())
                    .to(email)
                    .subject("Your Job Portal password reset code")
                    .body(emailBody)
                    .eventType("password.reset.otp")
                    .referenceId(savedOtp.getId())
                    .build());
            log.info("Password reset OTP generated and email requested. userId={}, otpId={}", user.getId(), savedOtp.getId());
        } catch (FeignException ex) {
            savedOtp.setUsed(true);
            savedOtp.setUsedAt(LocalDateTime.now());
            passwordResetOtpRepository.save(savedOtp);
            log.error("Password reset OTP email request failed. userId={}, otpId={}", user.getId(), savedOtp.getId(), ex);
            throw new ServiceUnavailableException("Unable to send OTP email right now. Please try again.");
        }
    }

    @Override
    public void resetPasswordWithOtp(ResetPasswordWithOtpRequest request) {
        String email = request.getEmail().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Invalid or expired OTP"));

        PasswordResetOtp otp = passwordResetOtpRepository
                .findTopByEmailAndOtpCodeAndUsedFalseOrderByCreatedAtDesc(email, request.getOtp())
                .orElseThrow(() -> new BadRequestException("Invalid or expired OTP"));

        LocalDateTime now = LocalDateTime.now();
        if (otp.getExpiresAt().isBefore(now)) {
            otp.setUsed(true);
            otp.setUsedAt(now);
            passwordResetOtpRepository.save(otp);
            log.warn("Expired OTP used for password reset attempt. userId={}, otpId={}", user.getId(), otp.getId());
            throw new BadRequestException("Invalid or expired OTP");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        otp.setUsed(true);
        otp.setUsedAt(now);
        passwordResetOtpRepository.save(otp);
        passwordResetOtpRepository.markAllActiveAsUsed(email, now);

        log.info("Password reset completed using OTP. userId={}, otpId={}", user.getId(), otp.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public InternalUserContactResponse getInternalUserContact(Long id) {
        User user = findUser(id);
        return InternalUserContactResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .active(user.isActive())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id, JwtUserPrincipal principal) {
        ensureSelfOrAdmin(id, principal);
        log.info("Fetching user by id. userId={}", id);
        return toUserResponse(findUser(id));
    }

    @Override
    public UserResponse updateUser(Long id, UpdateUserRequest request, JwtUserPrincipal principal) {
        ensureSelfOrAdmin(id, principal);

        User user = findUser(id);
        user.setName(request.getName());
        user.setPhone(request.getPhone());

        UserResponse response = toUserResponse(userRepository.save(user));
        log.info("User updated successfully. userId={}", id);
        return response;
    }

    @Override
    public void changePassword(Long id, ChangePasswordRequest request, JwtUserPrincipal principal) {
        ensureSelfOrAdmin(id, principal);

        User user = findUser(id);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.warn("Password change failed. Current password does not match. userId={}", id);
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed successfully. userId={}", id);
    }

    @Override
    public void deactivateUser(Long id) {
        User user = findUser(id);
        user.setActive(false);
        userRepository.save(user);

        log.info("User deactivated successfully. userId={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(Role role, Pageable pageable) {
        Page<User> page = role == null ? userRepository.findAll(pageable) : userRepository.findByRole(role, pageable);

        log.info("Users fetched successfully. page={}, size={}, total={}",
                pageable.getPageNumber(), pageable.getPageSize(), page.getTotalElements());

        return page.map(this::toUserResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId, JwtUserPrincipal principal) {
        ensureSelfOrAdmin(userId, principal);

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile", userId));

        log.info("User profile fetched successfully. userId={}", userId);
        return toProfileResponse(profile);
    }

    @Override
    public UserProfileResponse upsertProfile(Long userId, UserProfileRequest request, JwtUserPrincipal principal) {
        ensureSelfOrAdmin(userId, principal);

        User user = findUser(userId);
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElse(UserProfile.builder().user(user).experienceYrs(0).build());

        profile.setBio(request.getBio());
        profile.setSkills(request.getSkills());
        profile.setExperienceYrs(request.getExperienceYrs());
        profile.setCurrentCompany(request.getCurrentCompany());
        profile.setLocation(request.getLocation());
        profile.setLinkedinUrl(request.getLinkedinUrl());
        profile.setGithubUrl(request.getGithubUrl());

        UserProfileResponse response = toProfileResponse(userProfileRepository.save(profile));
        log.info("User profile saved successfully. userId={}", userId);
        return response;
    }

    private AuthResponse buildAuthResponse(User user) {
        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .accessToken(jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name()))
                .refreshToken(jwtUtil.generateRefreshToken(user.getId(), user.getEmail(), user.getRole().name()))
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getExpirationInSeconds())
                .refreshExpiresIn(jwtUtil.getRefreshExpirationInSeconds())
                .build();
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private void ensureSelfOrAdmin(Long targetUserId, JwtUserPrincipal principal) {
        if (principal == null) {
            log.warn("Unauthorized access. Authentication is required.");
            throw new UnauthorizedActionException("Authentication is required");
        }

        if (!principal.userId().equals(targetUserId) && !"ADMIN".equals(principal.role())) {
            log.warn("Unauthorized access. requesterUserId={}, targetUserId={}",
                    principal.userId(), targetUserId);
            throw new UnauthorizedActionException("You can access only your own account");
        }
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .phone(user.getPhone())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private UserProfileResponse toProfileResponse(UserProfile profile) {
        return UserProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .bio(profile.getBio())
                .skills(profile.getSkills())
                .experienceYrs(profile.getExperienceYrs())
                .currentCompany(profile.getCurrentCompany())
                .location(profile.getLocation())
                .linkedinUrl(profile.getLinkedinUrl())
                .githubUrl(profile.getGithubUrl())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private String generateOtp() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }

    private String buildOtpEmailBody(String name, String otp) {
        String safeName = (name == null || name.isBlank()) ? "User" : name;
        return "Hello " + safeName + ",\n\n"
                + "We received a request to reset the password for your Job Portal account.\n\n"
                + "One-Time Password (OTP): " + otp + "\n"
                + "Validity: " + otpExpiryMinutes + " minutes\n\n"
                + "Please use this OTP to complete your password reset. For your security, do not share it with anyone.\n\n"
                + "If you did not request a password reset, you can safely ignore this email.\n";
    }

}