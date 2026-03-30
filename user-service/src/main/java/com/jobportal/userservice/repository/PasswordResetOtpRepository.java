package com.jobportal.userservice.repository;

import com.jobportal.userservice.entity.PasswordResetOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {

    Optional<PasswordResetOtp> findTopByEmailAndOtpCodeAndUsedFalseOrderByCreatedAtDesc(String email, String otpCode);

    @Modifying
    @Query("""
            update PasswordResetOtp otp
               set otp.used = true,
                   otp.usedAt = :usedAt
             where otp.email = :email
               and otp.used = false
            """)
    int markAllActiveAsUsed(@Param("email") String email, @Param("usedAt") LocalDateTime usedAt);
}