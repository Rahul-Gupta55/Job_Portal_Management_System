package com.jobportal.jobservice.controller;

import com.jobportal.commonsecurity.security.JwtUserPrincipal;
import com.jobportal.jobservice.dto.ApiResponse;
import com.jobportal.jobservice.dto.CreateJobRequest;
import com.jobportal.jobservice.dto.JobResponse;
import com.jobportal.jobservice.dto.UpdateJobRequest;
import com.jobportal.jobservice.entity.JobStatus;
import com.jobportal.jobservice.entity.JobType;
import com.jobportal.jobservice.service.JobService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobControllerTest {

    @Mock
    private JobService jobService;

    @InjectMocks
    private JobController jobController;

    @Test
    void createJobReturnsCreatedResponse() {
        JwtUserPrincipal principal = new JwtUserPrincipal(11L, "recruiter@example.com", "RECRUITER");
        CreateJobRequest request = createRequest();
        JobResponse jobResponse = JobResponse.builder().id(1L).title("Java Developer").build();
        when(jobService.createJob(request, principal)).thenReturn(jobResponse);

        ResponseEntity<ApiResponse<JobResponse>> response = jobController.createJob(request, principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getMessage()).isEqualTo("Job created successfully");
        assertThat(response.getBody().getData()).isEqualTo(jobResponse);
    }

    @Test
    void getAllJobsBuildsSortedPageable() {
        when(jobService.getAllOpenJobs(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(JobResponse.builder().id(1L).build())));

        ResponseEntity<ApiResponse<org.springframework.data.domain.Page<JobResponse>>> response = jobController.getAllJobs(3, 15);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(jobService).getAllOpenJobs(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(3);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(15);
        assertThat(pageableCaptor.getValue().getSort().toString()).contains("createdAt: DESC");
        assertThat(response.getBody().getMessage()).isEqualTo("Jobs fetched successfully");
    }

    @Test
    void getJobByIdDelegatesWithPrincipal() {
        JwtUserPrincipal principal = new JwtUserPrincipal(11L, "recruiter@example.com", "RECRUITER");
        when(jobService.getJobById(5L, principal)).thenReturn(JobResponse.builder().id(5L).build());

        ResponseEntity<ApiResponse<JobResponse>> response = jobController.getJobById(5L, principal);

        assertThat(response.getBody().getMessage()).isEqualTo("Job fetched successfully");
        verify(jobService).getJobById(5L, principal);
    }

    @Test
    void updateJobDelegatesWithRequestAndPrincipal() {
        JwtUserPrincipal principal = new JwtUserPrincipal(11L, "recruiter@example.com", "RECRUITER");
        UpdateJobRequest request = updateRequest();
        when(jobService.updateJob(5L, request, principal)).thenReturn(JobResponse.builder().id(5L).status(JobStatus.OPEN).build());

        ResponseEntity<ApiResponse<JobResponse>> response = jobController.updateJob(5L, request, principal);

        assertThat(response.getBody().getMessage()).isEqualTo("Job updated successfully");
        verify(jobService).updateJob(5L, request, principal);
    }

    @Test
    void closeJobDelegates() {
        JwtUserPrincipal principal = new JwtUserPrincipal(11L, "recruiter@example.com", "RECRUITER");
        when(jobService.closeJob(5L, principal)).thenReturn(JobResponse.builder().id(5L).status(JobStatus.CLOSED).build());

        ResponseEntity<ApiResponse<JobResponse>> response = jobController.closeJob(5L, principal);

        assertThat(response.getBody().getMessage()).isEqualTo("Job closed successfully");
        verify(jobService).closeJob(5L, principal);
    }

    @Test
    void reopenJobDelegates() {
        JwtUserPrincipal principal = new JwtUserPrincipal(11L, "recruiter@example.com", "RECRUITER");
        when(jobService.reopenJob(5L, principal)).thenReturn(JobResponse.builder().id(5L).status(JobStatus.OPEN).build());

        ResponseEntity<ApiResponse<JobResponse>> response = jobController.reopenJob(5L, principal);

        assertThat(response.getBody().getMessage()).isEqualTo("Job reopened successfully");
        verify(jobService).reopenJob(5L, principal);
    }

    @Test
    void deleteDraftJobReturnsSuccessMessage() {
        JwtUserPrincipal principal = new JwtUserPrincipal(11L, "recruiter@example.com", "RECRUITER");

        ResponseEntity<ApiResponse<Void>> response = jobController.deleteDraftJob(5L, principal);

        assertThat(response.getBody().getMessage()).isEqualTo("Draft job deleted successfully");
        verify(jobService).deleteDraftJob(5L, principal);
    }

    @Test
    void getJobsByRecruiterDelegates() {
        JwtUserPrincipal principal = new JwtUserPrincipal(11L, "recruiter@example.com", "RECRUITER");
        when(jobService.getJobsByRecruiter(11L, principal)).thenReturn(List.of(JobResponse.builder().id(1L).build()));

        ResponseEntity<ApiResponse<List<JobResponse>>> response = jobController.getJobsByRecruiter(11L, principal);

        assertThat(response.getBody().getMessage()).isEqualTo("Recruiter jobs fetched successfully");
        assertThat(response.getBody().getData()).hasSize(1);
    }

    @Test
    void getSkillsDelegates() {
        JwtUserPrincipal principal = new JwtUserPrincipal(11L, "recruiter@example.com", "RECRUITER");
        when(jobService.getSkills(5L, principal)).thenReturn(List.of("Java", "Spring"));

        ResponseEntity<ApiResponse<List<String>>> response = jobController.getSkills(5L, principal);

        assertThat(response.getBody().getMessage()).isEqualTo("Job skills fetched successfully");
        assertThat(response.getBody().getData()).containsExactly("Java", "Spring");
    }

    private CreateJobRequest createRequest() {
        return CreateJobRequest.builder()
                .title("Java Developer")
                .company("Acme")
                .location("Hyderabad")
                .description("A".repeat(60))
                .jobType(JobType.FULL_TIME)
                .status(JobStatus.OPEN)
                .skills(List.of("Java", "Spring"))
                .experienceMin(2)
                .experienceMax(5)
                .deadline(LocalDate.now().plusDays(15))
                .build();
    }

    private UpdateJobRequest updateRequest() {
        return UpdateJobRequest.builder()
                .title("Senior Java Developer")
                .company("Acme")
                .location("Bangalore")
                .description("B".repeat(60))
                .jobType(JobType.FULL_TIME)
                .status(JobStatus.OPEN)
                .skills(List.of("Java", "Spring Boot"))
                .experienceMin(3)
                .experienceMax(6)
                .deadline(LocalDate.now().plusDays(20))
                .build();
    }
}
