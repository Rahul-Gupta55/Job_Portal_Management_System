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
import com.jobportal.userservice.exception.ServiceUnavailableException;
import com.jobportal.userservice.exception.UnauthorizedActionException;
import com.jobportal.userservice.repository.PasswordResetOtpRepository;
import com.jobportal.userservice.repository.UserProfileRepository;
import com.jobportal.userservice.repository.UserRepository;
import feign.FeignException;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.time.LocalDateTime;
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
    private PasswordResetOtpRepository passwordResetOtpRepository;
    @Mock
    private NotificationServiceClient notificationServiceClient;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "otpExpiryMinutes", 10L);
        ReflectionTestUtils.setField(userService, "internalApiKey", "jobportal-internal-key");
    }

    @Test
    void registerCreatesUserAndDefaultProfile() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Alice")
                .email("Alice@Example.com")
                .password("Password1")
                .role(Role.JOB_SEEKER)
                .phone("999")
                .build();
        when(userRepository.existsByEmail("Alice@Example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(11L);
            return user;
        });
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.register(request);

        assertThat(response.getId()).isEqualTo(11L);
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        verify(userProfileRepository).save(any(UserProfile.class));
    }

    @Test
    void registerRejectsDuplicateEmail() {
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
    void loginAuthenticatesAndBuildsTokens() {
        LoginRequest request = LoginRequest.builder().email("alice@example.com").password("Password1").build();
        User user = User.builder().id(1L).email("alice@example.com").role(Role.JOB_SEEKER).build();
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateAccessToken(1L, "alice@example.com", "JOB_SEEKER")).thenReturn("access");
        when(jwtUtil.generateRefreshToken(1L, "alice@example.com", "JOB_SEEKER")).thenReturn("refresh");
        when(jwtUtil.getExpirationInSeconds()).thenReturn(3600L);
        when(jwtUtil.getRefreshExpirationInSeconds()).thenReturn(86400L);

        AuthResponse response = userService.login(request);

        verify(authenticationManager).authenticate(new UsernamePasswordAuthenticationToken("alice@example.com", "Password1"));
        assertThat(response.getAccessToken()).isEqualTo("access");
        assertThat(response.getRefreshToken()).isEqualTo("refresh");
    }

    @Test
    void refreshTokenReturnsNewTokens() {
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");
        Claims claims = mock(Claims.class);
        when(claims.get("userId")).thenReturn(9);
        when(claims.get("role", String.class)).thenReturn("JOB_SEEKER");
        when(claims.getSubject()).thenReturn("alice@example.com");
        when(jwtUtil.refreshToken("refresh-token")).thenReturn("new-access");
        when(jwtUtil.extractClaims("refresh-token")).thenReturn(claims);
        when(jwtUtil.generateRefreshToken(9L, "alice@example.com", "JOB_SEEKER")).thenReturn("new-refresh");
        when(jwtUtil.getExpirationInSeconds()).thenReturn(3600L);
        when(jwtUtil.getRefreshExpirationInSeconds()).thenReturn(86400L);

        AuthResponse response = userService.refreshToken(request);

        assertThat(response.getUserId()).isEqualTo(9L);
        assertThat(response.getAccessToken()).isEqualTo("new-access");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
    }

    @Test
    void requestPasswordResetOtpReturnsSilentlyForUnknownEmail() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder().email("missing@example.com").build();
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        userService.requestPasswordResetOtp(request);

        verify(passwordResetOtpRepository, never()).save(any());
        verify(notificationServiceClient, never()).sendEmail(any(), any());
    }

    @Test
    void requestPasswordResetOtpStoresOtpAndCallsNotificationService() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder().email("alice@example.com").build();
        User user = User.builder().id(5L).name("Alice").email("alice@example.com").role(Role.JOB_SEEKER).build();
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordResetOtpRepository.save(any(PasswordResetOtp.class))).thenAnswer(invocation -> {
            PasswordResetOtp otp = invocation.getArgument(0);
            if (otp.getId() == null) {
                otp.setId(22L);
            }
            return otp;
        });

        userService.requestPasswordResetOtp(request);

        verify(passwordResetOtpRepository).markAllActiveAsUsed(eq("alice@example.com"), any(LocalDateTime.class));

        ArgumentCaptor<InternalEmailRequest> emailCaptor = ArgumentCaptor.forClass(InternalEmailRequest.class);
        verify(notificationServiceClient).sendEmail(eq("jobportal-internal-key"), emailCaptor.capture());
        assertThat(emailCaptor.getValue().getTo()).isEqualTo("alice@example.com");
        assertThat(emailCaptor.getValue().getBody()).contains("reset the password for your Job Portal account");
        assertThat(emailCaptor.getValue().getBody()).contains("10 minutes");
    }

    @Test
    void requestPasswordResetOtpMarksOtpUsedWhenEmailDispatchFails() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder().email("alice@example.com").build();
        User user = User.builder().id(5L).name("Alice").email("alice@example.com").role(Role.JOB_SEEKER).build();
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordResetOtpRepository.save(any(PasswordResetOtp.class))).thenAnswer(invocation -> {
            PasswordResetOtp otp = invocation.getArgument(0);
            if (otp.getId() == null) {
                otp.setId(33L);
            }
            return otp;
        });
        doThrow(feignException()).when(notificationServiceClient).sendEmail(any(), any());

        assertThatThrownBy(() -> userService.requestPasswordResetOtp(request))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("Unable to send OTP email right now");

        ArgumentCaptor<PasswordResetOtp> otpCaptor = ArgumentCaptor.forClass(PasswordResetOtp.class);
        verify(passwordResetOtpRepository, times(2)).save(otpCaptor.capture());
        assertThat(otpCaptor.getAllValues().get(1).isUsed()).isTrue();
        assertThat(otpCaptor.getAllValues().get(1).getUsedAt()).isNotNull();
    }

    @Test
    void resetPasswordWithOtpUpdatesPasswordAndConsumesOtp() {
        ResetPasswordWithOtpRequest request = ResetPasswordWithOtpRequest.builder()
                .email("alice@example.com")
                .otp("123456")
                .newPassword("NewPass1")
                .build();
        User user = User.builder().id(5L).email("alice@example.com").password("old-encoded").role(Role.JOB_SEEKER).build();
        PasswordResetOtp otp = PasswordResetOtp.builder()
                .id(7L)
                .userId(5L)
                .email("alice@example.com")
                .otpCode("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .build();
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordResetOtpRepository.findTopByEmailAndOtpCodeAndUsedFalseOrderByCreatedAtDesc("alice@example.com", "123456"))
                .thenReturn(Optional.of(otp));
        when(passwordEncoder.encode("NewPass1")).thenReturn("new-encoded");

        userService.resetPasswordWithOtp(request);

        assertThat(user.getPassword()).isEqualTo("new-encoded");
        assertThat(otp.isUsed()).isTrue();
        assertThat(otp.getUsedAt()).isNotNull();
        verify(passwordResetOtpRepository).markAllActiveAsUsed(eq("alice@example.com"), any(LocalDateTime.class));
    }

    @Test
    void resetPasswordWithOtpRejectsExpiredOtp() {
        ResetPasswordWithOtpRequest request = ResetPasswordWithOtpRequest.builder()
                .email("alice@example.com")
                .otp("123456")
                .newPassword("NewPass1")
                .build();
        User user = User.builder().id(5L).email("alice@example.com").password("old-encoded").role(Role.JOB_SEEKER).build();
        PasswordResetOtp otp = PasswordResetOtp.builder()
                .id(7L)
                .userId(5L)
                .email("alice@example.com")
                .otpCode("123456")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .used(false)
                .build();
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordResetOtpRepository.findTopByEmailAndOtpCodeAndUsedFalseOrderByCreatedAtDesc("alice@example.com", "123456"))
                .thenReturn(Optional.of(otp));

        assertThatThrownBy(() -> userService.resetPasswordWithOtp(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid or expired OTP");

        verify(userRepository, never()).save(any(User.class));
        verify(passwordResetOtpRepository).save(otp);
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

        assertThat(response.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void getUserByIdRejectsDifferentUserWithoutAdminRole() {
        JwtUserPrincipal principal = new JwtUserPrincipal(6L, "other@example.com", "JOB_SEEKER");

        assertThatThrownBy(() -> userService.getUserById(5L, principal))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() {
        // 1. Arrange
        Long targetUserId = 5L;
        String oldEncodedPassword = "encoded-old";

        User user = User.builder()
                .id(targetUserId)
                .password(oldEncodedPassword)
                .email("alice@example.com")
                .role(Role.JOB_SEEKER)
                .build();

        // Ensure the principal matches the ID to get past the "Access Denied" check
        JwtUserPrincipal principal = new JwtUserPrincipal(targetUserId, "alice@example.com", "JOB_SEEKER");

        ChangePasswordRequest request = new ChangePasswordRequest("WrongPass1", "NewPass1");

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass1", oldEncodedPassword)).thenReturn(false);

        // 2. Act & Assert
        assertThatThrownBy(() -> userService.changePassword(targetUserId, request, principal))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Current password is incorrect");

        // 3. Verification (Optional but recommended)
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void listUsersReturnsFilteredPage() {
        User recruiter = User.builder().id(1L).email("r@example.com").name("Recruiter").role(Role.RECRUITER).active(true).build();
        when(userRepository.findByRole(eq(Role.RECRUITER), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(recruiter)));

        var page = userService.listUsers(Role.RECRUITER, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getRole()).isEqualTo("RECRUITER");
    }

    private FeignException feignException() {
        var response = feign.Response.builder()
                .status(503)
                .reason("mail down")
                .request(feign.Request.create(feign.Request.HttpMethod.POST, URI.create("http://notification-service/api/internal/notifications/email").toString(), java.util.Map.of(), null, java.nio.charset.StandardCharsets.UTF_8, null))
                .headers(java.util.Map.of())
                .body(new byte[0])
                .build();
        return FeignException.errorStatus("NotificationServiceClient#sendEmail", response);
    }

    @Test
    void getInternalUserContactReturnsUserContactData() {
        User user = User.builder()
                .id(5L)
                .name("Alice")
                .email("alice@example.com")
                .role(Role.JOB_SEEKER)
                .active(true)
                .build();
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        InternalUserContactResponse response = userService.getInternalUserContact(5L);

        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        assertThat(response.isActive()).isTrue();
    }

}