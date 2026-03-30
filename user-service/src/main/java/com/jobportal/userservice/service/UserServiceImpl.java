package com.jobportal.userservice.service;

import com.jobportal.userservice.dto.*;
import com.jobportal.userservice.entity.Role;
import com.jobportal.userservice.entity.User;
import com.jobportal.userservice.entity.UserProfile;
import com.jobportal.userservice.exception.BadRequestException;
import com.jobportal.userservice.exception.DuplicateEmailException;
import com.jobportal.userservice.exception.ResourceNotFoundException;
import com.jobportal.userservice.exception.UnauthorizedActionException;
import com.jobportal.userservice.repository.UserProfileRepository;
import com.jobportal.userservice.repository.UserRepository;
import com.jobportal.commonsecurity.security.JwtUserPrincipal;
import com.jobportal.commonsecurity.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

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
}