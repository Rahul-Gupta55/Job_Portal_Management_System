package com.jobportal.resumeservice.controller;

import com.jobportal.commonsecurity.security.JwtUserPrincipal;
import com.jobportal.resumeservice.dto.ApiResponse;
import com.jobportal.resumeservice.dto.ResumeResponse;
import com.jobportal.resumeservice.service.ResumeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeControllerTest {

    @Mock
    private ResumeService resumeService;

    @InjectMocks
    private ResumeController resumeController;

    @Test
    void createReturnsCreatedResponse() {
        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "user@example.com", "JOB_SEEKER");
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", "pdf".getBytes());
        ResumeResponse resumeResponse = ResumeResponse.builder().id(1L).fileName("resume.pdf").build();
        when(resumeService.create(file, true, principal)).thenReturn(resumeResponse);

        ResponseEntity<ApiResponse<ResumeResponse>> response = resumeController.create(file, true, principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getMessage()).isEqualTo("Resume uploaded successfully");
        assertThat(response.getBody().getData()).isEqualTo(resumeResponse);
    }

    @Test
    void getByIdDelegates() {
        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "user@example.com", "JOB_SEEKER");
        when(resumeService.getById(1L, principal)).thenReturn(ResumeResponse.builder().id(1L).build());

        ResponseEntity<ApiResponse<ResumeResponse>> response = resumeController.getById(1L, principal);

        assertThat(response.getBody().getMessage()).isEqualTo("Resume fetched successfully");
    }

    @Test
    void getByUserIdDelegates() {
        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "user@example.com", "JOB_SEEKER");
        when(resumeService.getByUserId(5L, principal)).thenReturn(List.of(ResumeResponse.builder().id(1L).build()));

        ResponseEntity<ApiResponse<List<ResumeResponse>>> response = resumeController.getByUserId(5L, principal);

        assertThat(response.getBody().getMessage()).isEqualTo("Resumes fetched successfully");
        assertThat(response.getBody().getData()).hasSize(1);
    }

    @Test
    void setPrimaryDelegates() {
        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "user@example.com", "JOB_SEEKER");
        when(resumeService.setPrimary(1L, principal)).thenReturn(ResumeResponse.builder().id(1L).primary(true).build());

        ResponseEntity<ApiResponse<ResumeResponse>> response = resumeController.setPrimary(1L, principal);

        assertThat(response.getBody().getMessage()).isEqualTo("Primary resume updated successfully");
    }

    @Test
    void downloadFileReturnsPdfResourceAndAttachmentHeader() {
        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "user@example.com", "JOB_SEEKER");
        ResumeResponse resumeResponse = ResumeResponse.builder().id(1L).fileName("resume.pdf").build();
        ByteArrayResource resource = new ByteArrayResource("content".getBytes());
        when(resumeService.getById(1L, principal)).thenReturn(resumeResponse);
        when(resumeService.downloadFile(1L, principal)).thenReturn(resource);

        ResponseEntity<org.springframework.core.io.Resource> response = resumeController.downloadFile(1L, principal);

        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).contains("resume.pdf");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(resource);
    }

    @Test
    void deleteReturnsSuccessMessage() {
        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "user@example.com", "JOB_SEEKER");

        ResponseEntity<ApiResponse<Void>> response = resumeController.delete(1L, principal);

        assertThat(response.getBody().getMessage()).isEqualTo("Resume deleted successfully");
        verify(resumeService).delete(1L, principal);
    }
}
