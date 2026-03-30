package com.jobportal.userservice.service;

import com.jobportal.commonsecurity.security.JwtUserPrincipal;
import com.jobportal.commonsecurity.security.JwtUtil;
import com.jobportal.userservice.dto.*;
import com.jobportal.userservice.entity.Role;
import com.jobportal.userservice.entity.User;
import com.jobportal.userservice.entity.UserProfile;
import com.jobportal.userservice.exception.BadRequestException;
import com.jobportal.userservice.exception.DuplicateEmailException;
import com.jobportal.userservice.exception.UnauthorizedActionException;
import com.jobportal.userservice.repository.UserProfileRepository;
import com.jobportal.userservice.repository.UserRepository;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void registerCreatesUserAndDefaultProfile() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Alice")
                .email("Alice@Example.com")
                .password("Password1")
                .role(Role.JOB_SEEKER)
                .phone("9999999999")
                .build();

        when(userRepository.existsByEmail("Alice@Example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.register(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        assertThat(response.getRole()).isEqualTo("JOB_SEEKER");
        verify(userProfileRepository).save(any(UserProfile.class));
    }

    @Test
    void registerThrowsWhenEmailAlreadyExists() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Alice")
                .email("alice@example.com")
                .password("Password1")
                .role(Role.JOB_SEEKER)
                .build();

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void registerRejectsAdminRegistration() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Admin")
                .email("admin@example.com")
                .password("Password1")
                .role(Role.ADMIN)
                .build();

        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessageContaining("ADMIN registration is not allowed");
    }

    @Test
    void loginAuthenticatesUserAndReturnsJwtResponse() {
        LoginRequest request = LoginRequest.builder()
                .email("recruiter@example.com")
                .password("Password1")
                .build();

        User user = User.builder()
                .id(7L)
                .email("recruiter@example.com")
                .role(Role.RECRUITER)
                .build();

        when(userRepository.findByEmail("recruiter@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateAccessToken(7L, "recruiter@example.com", "RECRUITER")).thenReturn("jwt-token");
        when(jwtUtil.generateRefreshToken(7L, "recruiter@example.com", "RECRUITER")).thenReturn("refresh-token");
        when(jwtUtil.getExpirationInSeconds()).thenReturn(3600L);
        when(jwtUtil.getRefreshExpirationInSeconds()).thenReturn(604800L);

        AuthResponse response = userService.login(request);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getRole()).isEqualTo("RECRUITER");
    }

    @Test
    void refreshTokenReturnsNewAccessAndRefreshTokens() {
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");
        Claims claims = mock(Claims.class);

        when(jwtUtil.refreshToken("refresh-token")).thenReturn("new-access-token");
        when(jwtUtil.extractClaims("refresh-token")).thenReturn(claims);
        when(claims.get("userId")).thenReturn(7L);
        when(claims.getSubject()).thenReturn("recruiter@example.com");
        when(claims.get("role", String.class)).thenReturn("RECRUITER");
        when(jwtUtil.generateRefreshToken(7L, "recruiter@example.com", "RECRUITER"))
                .thenReturn("rotated-refresh-token");
        when(jwtUtil.getExpirationInSeconds()).thenReturn(3600L);
        when(jwtUtil.getRefreshExpirationInSeconds()).thenReturn(604800L);

        AuthResponse response = userService.refreshToken(request);

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("rotated-refresh-token");
        assertThat(response.getUserId()).isEqualTo(7L);
        assertThat(response.getEmail()).isEqualTo("recruiter@example.com");
    }

    @Test
    void refreshTokenWrapsJwtErrorsAsBadRequest() {
        RefreshTokenRequest request = new RefreshTokenRequest("bad-token");
        when(jwtUtil.refreshToken("bad-token")).thenThrow(new IllegalArgumentException("Invalid refresh token"));

        assertThatThrownBy(() -> userService.refreshToken(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    void getUserByIdRejectsDifferentUserWhenNotAdmin() {
        JwtUserPrincipal principal = new JwtUserPrincipal(99L, "other@example.com", "JOB_SEEKER");

        assertThatThrownBy(() -> userService.getUserById(1L, principal))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() {
        User user = User.builder()
                .id(5L)
                .password("encoded-old")
                .role(Role.JOB_SEEKER)
                .build();

        ChangePasswordRequest request = new ChangePasswordRequest("wrong", "NewPassword1");
        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "user@example.com", "JOB_SEEKER");

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded-old")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(5L, request, principal))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Current password is incorrect");
    }

    @Test
    void upsertProfileCreatesProfileWhenMissing() {
        Long userId = 5L;
        JwtUserPrincipal principal = new JwtUserPrincipal(userId, "user@example.com", "JOB_SEEKER");
        User user = User.builder().id(userId).email("user@example.com").role(Role.JOB_SEEKER).build();

        UserProfileRequest request = UserProfileRequest.builder()
                .bio("Java backend developer")
                .skills("Java, Spring Boot")
                .experienceYrs(2)
                .currentCompany("Acme")
                .location("Hyderabad")
                .linkedinUrl("https://linkedin.com/in/test")
                .githubUrl("https://github.com/test")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> {
            UserProfile profile = invocation.getArgument(0);
            profile.setId(10L);
            return profile;
        });

        UserProfileResponse response = userService.upsertProfile(userId, request, principal);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getSkills()).isEqualTo("Java, Spring Boot");
        assertThat(response.getExperienceYrs()).isEqualTo(2);
    }

    @Test
    void listUsersFiltersByRole() {
        User recruiter = User.builder()
                .id(1L)
                .name("Recruiter")
                .email("r@example.com")
                .role(Role.RECRUITER)
                .active(true)
                .build();

        when(userRepository.findByRole(eq(Role.RECRUITER), any())).thenReturn(new PageImpl<>(List.of(recruiter)));

        var page = userService.listUsers(Role.RECRUITER, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getRole()).isEqualTo("RECRUITER");
    }


    @Test
    void getUserByIdReturnsSelfUser() {
        User user = User.builder()
                .id(5L)
                .name("Alice")
                .email("alice@example.com")
                .role(Role.JOB_SEEKER)
                .active(true)
                .build();
        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "alice@example.com", "JOB_SEEKER");
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        UserResponse response = userService.getUserById(5L, principal);

        assertThat(response)
                .extracting(UserResponse::getId, UserResponse::getEmail, UserResponse::getRole)
                .containsExactly(5L, "alice@example.com", "JOB_SEEKER");
    }

    @Test
    void updateUserPersistsNameAndPhone() {
        User user = User.builder()
                .id(5L)
                .name("Alice")
                .email("alice@example.com")
                .phone("1111111111")
                .role(Role.JOB_SEEKER)
                .active(true)
                .build();
        UpdateUserRequest request = new UpdateUserRequest("Alice Updated", "9999999999");
        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "alice@example.com", "JOB_SEEKER");
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateUser(5L, request, principal);

        assertThat(response)
                .extracting(UserResponse::getName, UserResponse::getPhone)
                .containsExactly("Alice Updated", "9999999999");
    }

    @Test
    void changePasswordEncodesAndSavesNewPassword() {
        User user = User.builder()
                .id(5L)
                .password("encoded-old")
                .role(Role.JOB_SEEKER)
                .build();
        ChangePasswordRequest request = new ChangePasswordRequest("old-password", "NewPassword1");
        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "user@example.com", "JOB_SEEKER");

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-password", "encoded-old")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword1")).thenReturn("encoded-new");

        userService.changePassword(5L, request, principal);

        assertThat(user.getPassword()).isEqualTo("encoded-new");
        verify(userRepository).save(user);
    }

    @Test
    void deactivateUserMarksUserInactive() {
        User user = User.builder()
                .id(5L)
                .active(true)
                .role(Role.JOB_SEEKER)
                .build();
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        userService.deactivateUser(5L);

        assertThat(user.isActive()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void getProfileReturnsExistingProfile() {
        Long userId = 5L;
        User user = User.builder().id(userId).email("user@example.com").role(Role.JOB_SEEKER).build();
        UserProfile profile = UserProfile.builder()
                .id(9L)
                .user(user)
                .bio("Java backend developer")
                .skills("Java, Spring Boot")
                .experienceYrs(3)
                .build();
        JwtUserPrincipal principal = new JwtUserPrincipal(userId, "user@example.com", "JOB_SEEKER");
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        UserProfileResponse response = userService.getProfile(userId, principal);

        assertThat(response)
                .extracting(UserProfileResponse::getId, UserProfileResponse::getSkills, UserProfileResponse::getExperienceYrs)
                .containsExactly(9L, "Java, Spring Boot", 3);
    }

    @Test
    void upsertProfileUpdatesExistingProfile() {
        Long userId = 5L;
        JwtUserPrincipal principal = new JwtUserPrincipal(userId, "user@example.com", "JOB_SEEKER");
        User user = User.builder().id(userId).email("user@example.com").role(Role.JOB_SEEKER).build();
        UserProfile existingProfile = UserProfile.builder().id(10L).user(user).experienceYrs(1).build();
        UserProfileRequest request = UserProfileRequest.builder()
                .bio("Updated bio")
                .skills("Java, Spring")
                .experienceYrs(4)
                .currentCompany("Acme")
                .location("Hyderabad")
                .linkedinUrl("https://linkedin.com/in/test")
                .githubUrl("https://github.com/test")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existingProfile));
        when(userProfileRepository.save(existingProfile)).thenReturn(existingProfile);

        UserProfileResponse response = userService.upsertProfile(userId, request, principal);

        assertThat(response)
                .extracting(UserProfileResponse::getId, UserProfileResponse::getBio, UserProfileResponse::getExperienceYrs)
                .containsExactly(10L, "Updated bio", 4);
    }

    @Test
    void listUsersWithoutRoleReturnsAllUsers() {
        User user = User.builder()
                .id(2L)
                .name("Alice")
                .email("alice@example.com")
                .role(Role.JOB_SEEKER)
                .active(true)
                .build();
        when(userRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));

        var page = userService.listUsers(null, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getEmail()).isEqualTo("alice@example.com");
    }
}