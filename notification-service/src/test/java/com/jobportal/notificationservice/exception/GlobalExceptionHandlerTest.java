package com.jobportal.notificationservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFoundReturns404() {
        var response = handler.handleNotFound(new ResourceNotFoundException("Notification", 1L), request());
        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void handleForbiddenReturns403() {
        var response = handler.handleForbidden(new AccessDeniedException("denied"), request());
        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void handleGenericReturns500() {
        var response = handler.handleGeneric(new RuntimeException("boom"), request());
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
    }

    private HttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/notifications/1");
        return request;
    }
}
