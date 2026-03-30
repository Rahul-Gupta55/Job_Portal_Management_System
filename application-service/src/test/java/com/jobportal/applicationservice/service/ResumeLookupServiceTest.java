package com.jobportal.applicationservice.service;

import com.jobportal.applicationservice.client.ResumeServiceClient;
import com.jobportal.applicationservice.dto.ApiResponse;
import com.jobportal.applicationservice.dto.ResumeSummaryResponse;
import com.jobportal.applicationservice.exception.BadRequestException;
import com.jobportal.applicationservice.exception.ServiceUnavailableException;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeLookupServiceTest {

    @Mock
    private ResumeServiceClient resumeServiceClient;

    @InjectMocks
    private ResumeLookupService resumeLookupService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAccessibleResumeReturnsDataAndPassesBearerHeader() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user", "token-123"));
        ResumeSummaryResponse summary = ResumeSummaryResponse.builder().id(10L).userId(7L).fileName("resume.pdf").build();
        when(resumeServiceClient.getResumeById(anyString(), anyLong())).thenReturn(ApiResponse.of("ok", summary));

        ResumeSummaryResponse response = resumeLookupService.getAccessibleResume(10L);

        ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
        verify(resumeServiceClient).getResumeById(headerCaptor.capture(), org.mockito.ArgumentMatchers.eq(10L));
        assertThat(headerCaptor.getValue()).isEqualTo("Bearer token-123");
        assertThat(response).isEqualTo(summary);
    }

    @Test
    void getAccessibleResumeThrowsWhenResponseIsEmpty() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user", "token-123"));
        when(resumeServiceClient.getResumeById(anyString(), anyLong())).thenReturn(ApiResponse.of("ok", null));

        assertThatThrownBy(() -> resumeLookupService.getAccessibleResume(10L))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("empty response");
    }

    @Test
    void getAccessibleResumeMapsNotFoundToBadRequest() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user", "token-123"));
        when(resumeServiceClient.getResumeById(anyString(), anyLong())).thenThrow(feignException(404, "Not Found"));

        assertThatThrownBy(() -> resumeLookupService.getAccessibleResume(10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Resume does not exist");
    }

    @Test
    void getAccessibleResumeMapsForbiddenToBadRequest() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user", "token-123"));
        when(resumeServiceClient.getResumeById(anyString(), anyLong())).thenThrow(feignException(403, "Forbidden"));

        assertThatThrownBy(() -> resumeLookupService.getAccessibleResume(10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Resume does not exist");
    }

    @Test
    void getAccessibleResumeMapsOtherFeignErrorsToServiceUnavailable() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user", "token-123"));
        when(resumeServiceClient.getResumeById(anyString(), anyLong())).thenThrow(feignException(500, "Server Error"));

        assertThatThrownBy(() -> resumeLookupService.getAccessibleResume(10L))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("temporarily unavailable");
    }

    @Test
    void fallbackPropagatesBadRequestException() {
        BadRequestException error = new BadRequestException("resume invalid");

        assertThatThrownBy(() -> resumeLookupService.fallback(10L, error))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("resume invalid");
    }

    @Test
    void fallbackReturnsServiceUnavailableForOtherErrors() {
        RuntimeException error = new RuntimeException("boom");

        assertThatThrownBy(() -> resumeLookupService.fallback(10L, error))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("temporarily unavailable");
    }

    @Test
    void getAccessibleResumeThrowsWhenAuthenticationContextMissing() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> resumeLookupService.getAccessibleResume(10L))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("Authenticated request context is missing");
    }

    private FeignException feignException(int status, String reason) {
        Request request = Request.create(Request.HttpMethod.GET, "/api/resumes/10", Map.of(), null, StandardCharsets.UTF_8, null);
        Response response = Response.builder().status(status).reason(reason).request(request).headers(Map.of()).build();
        return FeignException.errorStatus("ResumeServiceClient#getResumeById", response);
    }
}
