package com.jobportal.applicationservice.service;

import com.jobportal.applicationservice.client.ResumeServiceClient;
import com.jobportal.applicationservice.dto.ResumeSummaryResponse;
import com.jobportal.applicationservice.exception.BadRequestException;
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
public class ResumeLookupService {

    private final ResumeServiceClient resumeServiceClient;

    @CircuitBreaker(name = "resumeService", fallbackMethod = "fallback")
    @Retry(name = "resumeService")
    public ResumeSummaryResponse getAccessibleResume(Long resumeId) {
        try {
            var response = resumeServiceClient.getResumeById(currentAuthorizationHeader(), resumeId);
            if (response == null || response.getData() == null) {
                throw new ServiceUnavailableException("Resume service returned an empty response");
            }
            return response.getData();
        } catch (FeignException.NotFound | FeignException.Forbidden ex) {
            throw new BadRequestException("Resume does not exist or does not belong to the authenticated user");
        } catch (FeignException ex) {
            throw new ServiceUnavailableException("Resume service is temporarily unavailable");
        }
    }

    public ResumeSummaryResponse fallback(Long resumeId, Throwable throwable) {
        if (throwable instanceof BadRequestException badRequestException) {
            throw badRequestException;
        }
        log.warn("Resume service fallback triggered. resumeId={}", resumeId, throwable);
        throw new ServiceUnavailableException("Resume service is temporarily unavailable");
    }

    private String currentAuthorizationHeader() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getCredentials() == null) {
            throw new ServiceUnavailableException("Authenticated request context is missing");
        }
        return "Bearer " + authentication.getCredentials().toString();
    }
}
