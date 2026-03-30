package com.jobportal.userservice.dto;

import com.jobportal.userservice.entity.Role;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void changePasswordRequestAcceptsPasswordWithUppercaseAndDigit() {
        ChangePasswordRequest request = new ChangePasswordRequest("OldPass1", "NewPass1");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void changePasswordRequestRejectsPasswordWithoutDigit() {
        ChangePasswordRequest request = new ChangePasswordRequest("OldPass1", "NewPassword");

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("newPassword");
    }

    @Test
    void registerRequestAcceptsPasswordWithUppercaseAndDigit() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Alice")
                .email("alice@example.com")
                .password("Secure1A")
                .role(Role.JOB_SEEKER)
                .phone("9999999999")
                .build();

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void registerRequestRejectsPasswordWithoutUppercase() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Alice")
                .email("alice@example.com")
                .password("secure123")
                .role(Role.JOB_SEEKER)
                .phone("9999999999")
                .build();

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("password");
    }
}
