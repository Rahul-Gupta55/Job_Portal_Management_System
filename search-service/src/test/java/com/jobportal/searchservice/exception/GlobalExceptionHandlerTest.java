package com.jobportal.searchservice.exception;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleRateLimitReturns429() {
        var response = handler.handleRateLimit(mock(RequestNotPermitted.class), request());
        assertThat(response.getStatusCode().value()).isEqualTo(429);
        assertThat(response.getBody().getMessage()).isEqualTo("Too many requests");
    }

    @Test
    void handleGenericReturns500() {
        var response = handler.handleGeneric(new RuntimeException("boom"), request());
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
    }

    private HttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/search/jobs");
        return request;
    }
}
