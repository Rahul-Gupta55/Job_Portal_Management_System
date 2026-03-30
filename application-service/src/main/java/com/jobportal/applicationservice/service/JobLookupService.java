package com.jobportal.applicationservice.service;

import com.jobportal.applicationservice.client.JobServiceClient;
import com.jobportal.applicationservice.dto.JobSummaryResponse;
import com.jobportal.applicationservice.exception.ServiceUnavailableException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobLookupService {

    private final JobServiceClient jobServiceClient;

    @CircuitBreaker(name = "jobService", fallbackMethod = "fallback")
    @Retry(name = "jobService")
    public JobSummaryResponse getJob(Long jobId) {
        try {
            var response = jobServiceClient.getJobById(currentAuthorizationHeader(), jobId);
            if (response == null || response.getData() == null) {
                log.warn("Job lookup returned empty response. jobId={}", jobId);
                throw new ServiceUnavailableException("Job service returned an empty response");
            }

            log.info("Job fetched successfully. jobId={}", response.getData().getId());
            return response.getData();
        } catch (FeignException ex) {
            log.error("Failed to fetch job. jobId={}", jobId, ex);
            throw new ServiceUnavailableException("Job service is temporarily unavailable");
        }
    }

    public JobSummaryResponse fallback(Long jobId, Throwable throwable) {
        log.warn("Job service fallback triggered. jobId={}", jobId, throwable);
        throw new ServiceUnavailableException("Job service is temporarily unavailable");
    }

    private String currentAuthorizationHeader() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getCredentials() == null) {
            log.warn("Missing authenticated request context.");
            throw new ServiceUnavailableException("Authenticated request context is missing");
        }
        return "Bearer " + authentication.getCredentials().toString();
    }
}
