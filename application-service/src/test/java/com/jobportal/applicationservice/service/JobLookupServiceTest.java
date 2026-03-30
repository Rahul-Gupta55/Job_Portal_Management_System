package com.jobportal.applicationservice.service;

import com.jobportal.applicationservice.client.JobServiceClient;
import com.jobportal.applicationservice.dto.ApiResponse;
import com.jobportal.applicationservice.dto.JobSummaryResponse;
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
class JobLookupServiceTest {

    @Mock
    private JobServiceClient jobServiceClient;

    @InjectMocks
    private JobLookupService jobLookupService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getJobReturnsDataAndPassesAuthorizationHeader() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user", "token-123"));
        JobSummaryResponse summary = JobSummaryResponse.builder().id(7L).title("Java Developer").status("OPEN").recruiterId(11L).build();
        when(jobServiceClient.getJobById(anyString(), anyLong())).thenReturn(ApiResponse.of("ok", summary));

        JobSummaryResponse response = jobLookupService.getJob(7L);

        ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
        verify(jobServiceClient).getJobById(headerCaptor.capture(), org.mockito.ArgumentMatchers.eq(7L));
        assertThat(headerCaptor.getValue()).isEqualTo("Bearer token-123");
        assertThat(response).isEqualTo(summary);
    }

    @Test
    void getJobThrowsWhenResponseDataIsMissing() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user", "token-123"));
        when(jobServiceClient.getJobById(anyString(), anyLong())).thenReturn(ApiResponse.of("ok", null));

        assertThatThrownBy(() -> jobLookupService.getJob(7L))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("empty response");
    }

    @Test
    void getJobWrapsFeignExceptionAsServiceUnavailable() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user", "token-123"));
        when(jobServiceClient.getJobById(anyString(), anyLong())).thenThrow(feignException(503, "Service Unavailable"));

        assertThatThrownBy(() -> jobLookupService.getJob(7L))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("temporarily unavailable");
    }

    @Test
    void fallbackAlwaysThrowsServiceUnavailableException() {
        RuntimeException error = new RuntimeException("boom");

        assertThatThrownBy(() -> jobLookupService.fallback(5L, error))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("temporarily unavailable");
    }

    @Test
    void getJobThrowsWhenAuthenticationContextMissing() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> jobLookupService.getJob(7L))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("Authenticated request context is missing");
    }

    private FeignException feignException(int status, String reason) {
        Request request = Request.create(Request.HttpMethod.GET, "/api/jobs/7", Map.of(), null, StandardCharsets.UTF_8, null);
        Response response = Response.builder().status(status).reason(reason).request(request).headers(Map.of()).build();
        return FeignException.errorStatus("JobServiceClient#getJobById", response);
    }
}
