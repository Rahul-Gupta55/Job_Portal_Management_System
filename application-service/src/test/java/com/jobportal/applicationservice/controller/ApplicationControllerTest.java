package com.jobportal.applicationservice.controller;

import com.jobportal.applicationservice.dto.*;
import com.jobportal.applicationservice.entity.ApplicationStatus;
import com.jobportal.applicationservice.service.ApplicationService;
import com.jobportal.commonsecurity.security.JwtUserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationControllerTest {

    @Mock
    private ApplicationService applicationService;

    @InjectMocks
    private ApplicationController applicationController;

    @Test
    void applyReturnsCreatedResponse() {
        JwtUserPrincipal principal = new JwtUserPrincipal(7L, "seeker@example.com", "JOB_SEEKER");
        ApplyRequest request = ApplyRequest.builder().jobId(10L).resumeId(20L).coverLetter("Interested").build();
        ApplicationResponse applicationResponse = ApplicationResponse.builder().id(1L).jobId(10L).candidateId(7L).status(ApplicationStatus.APPLIED).build();
        when(applicationService.apply(request, principal)).thenReturn(applicationResponse);

        ResponseEntity<ApiResponse<ApplicationResponse>> response = applicationController.apply(request, principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getMessage()).isEqualTo("Applied successfully");
        assertThat(response.getBody().getData()).isEqualTo(applicationResponse);
    }

    @Test
    void getByIdDelegates() {
        JwtUserPrincipal principal = new JwtUserPrincipal(7L, "seeker@example.com", "JOB_SEEKER");
        when(applicationService.getById(1L, principal)).thenReturn(ApplicationResponse.builder().id(1L).build());

        ResponseEntity<ApiResponse<ApplicationResponse>> response = applicationController.getById(1L, principal);

        assertThat(response.getBody().getMessage()).isEqualTo("Application fetched successfully");
        verify(applicationService).getById(1L, principal);
    }

    @Test
    void getByJobIdDelegates() {
        JwtUserPrincipal principal = new JwtUserPrincipal(11L, "recruiter@example.com", "RECRUITER");
        when(applicationService.getByJobId(9L, principal)).thenReturn(List.of(ApplicationResponse.builder().id(1L).build()));

        ResponseEntity<ApiResponse<List<ApplicationResponse>>> response = applicationController.getByJobId(9L, principal);

        assertThat(response.getBody().getMessage()).isEqualTo("Applications fetched successfully");
        assertThat(response.getBody().getData()).hasSize(1);
    }

    @Test
    void getByCandidateDelegates() {
        JwtUserPrincipal principal = new JwtUserPrincipal(7L, "seeker@example.com", "JOB_SEEKER");
        when(applicationService.getByCandidateId(7L, principal)).thenReturn(List.of(ApplicationResponse.builder().id(1L).build()));

        ResponseEntity<ApiResponse<List<ApplicationResponse>>> response = applicationController.getByCandidate(7L, principal);

        assertThat(response.getBody().getMessage()).isEqualTo("Applications fetched successfully");
    }

    @Test
    void updateStatusDelegates() {
        JwtUserPrincipal principal = new JwtUserPrincipal(11L, "recruiter@example.com", "RECRUITER");
        StatusUpdateRequest request = new StatusUpdateRequest(ApplicationStatus.SHORTLISTED, "Looks good");
        when(applicationService.updateStatus(1L, request, principal)).thenReturn(ApplicationResponse.builder().id(1L).status(ApplicationStatus.SHORTLISTED).build());

        ResponseEntity<ApiResponse<ApplicationResponse>> response = applicationController.updateStatus(1L, request, principal);

        assertThat(response.getBody().getMessage()).isEqualTo("Application status updated successfully");
    }

    @Test
    void withdrawDelegates() {
        JwtUserPrincipal principal = new JwtUserPrincipal(7L, "seeker@example.com", "JOB_SEEKER");
        when(applicationService.withdraw(1L, principal)).thenReturn(ApplicationResponse.builder().id(1L).status(ApplicationStatus.WITHDRAWN).build());

        ResponseEntity<ApiResponse<ApplicationResponse>> response = applicationController.withdraw(1L, principal);

        assertThat(response.getBody().getMessage()).isEqualTo("Application withdrawn successfully");
    }

    @Test
    void getHistoryDelegates() {
        JwtUserPrincipal principal = new JwtUserPrincipal(7L, "seeker@example.com", "JOB_SEEKER");
        when(applicationService.getHistory(1L, principal)).thenReturn(List.of(ApplicationStatusHistoryResponse.builder().id(1L).newStatus("APPLIED").build()));

        ResponseEntity<ApiResponse<List<ApplicationStatusHistoryResponse>>> response = applicationController.getHistory(1L, principal);

        assertThat(response.getBody().getMessage()).isEqualTo("Application history fetched successfully");
        assertThat(response.getBody().getData()).hasSize(1);
    }
}
