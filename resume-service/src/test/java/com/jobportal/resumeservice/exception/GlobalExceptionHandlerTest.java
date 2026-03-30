package com.jobportal.resumeservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFoundReturns404() {
        var response = handler.handleNotFound(new ResourceNotFoundException("Resume", 1L), request());
        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void handleForbiddenReturns403() {
        var response = handler.handleForbidden(new AccessDeniedException("denied"), request());
        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void handleValidationReturnsFieldErrors() throws Exception {
        var response = handler.handleValidation(validationException("file", "File is required"), request());
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getFieldErrors()).containsEntry("file", "File is required");
    }

    @Test
    void handleGenericReturns500() {
        var response = handler.handleGeneric(new RuntimeException("boom"), request());
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
    }

    private HttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/resumes/1");
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
            // Intentionally empty: this method exists only to provide a MethodParameter for validation tests.
        }
    }
}
