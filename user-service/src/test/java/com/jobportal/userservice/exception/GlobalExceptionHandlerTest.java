package com.jobportal.userservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFoundReturns404() {
        var response = handler.handleNotFound(new ResourceNotFoundException("User", 9L), request());

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        Assertions.assertNotNull(response.getBody());
        assertThat(response.getBody().getMessage()).contains("User not found");
    }

    @Test
    void handleConflictReturns409() {
        var response = handler.handleConflict(new DuplicateEmailException("user@example.com"), request());

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().getMessage()).contains("user@example.com");
    }

    @Test
    void handleForbiddenReturns403() {
        var response = handler.handleForbidden(new AccessDeniedException("denied"), request());

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody().getMessage()).isEqualTo("denied");
    }

    @Test
    void handleBadRequestReturns400() {
        var response = handler.handleBadRequest(new BadCredentialsException("bad credentials"), request());

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        Assertions.assertNotNull(response.getBody());
        assertThat(response.getBody().getMessage()).isEqualTo("bad credentials");
    }

    @Test
    void handleServiceUnavailableReturns503() {
        var response = handler.handleServiceUnavailable(new ServiceUnavailableException("mail down"), request());

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody().getMessage()).isEqualTo("mail down");
    }

    @Test
    void handleValidationReturnsFieldErrors() throws Exception {
        MethodArgumentNotValidException exception = validationException("email", "Invalid email format");

        var response = handler.handleValidation(exception, request());

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("Input validation error");
        assertThat(response.getBody().getFieldErrors()).containsEntry("email", "Invalid email format");
    }

    @Test
    void handleGenericReturns500() {
        var response = handler.handleGeneric(new RuntimeException("boom"), request());

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
    }

    private HttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/users/1");
        return request;
    }

    private MethodArgumentNotValidException validationException(String field, String message) throws Exception {
        Method method = ValidationTarget.class.getDeclaredMethod("handle", String.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", field, message));
        return new MethodArgumentNotValidException(parameter, bindingResult);
    }

    static class ValidationTarget {
        @SuppressWarnings("unused")
        void handle(String value) {
            // no-op
        }
    }
}