package com.jobportal.applicationservice.service;

import com.jobportal.applicationservice.dto.*;
import com.jobportal.applicationservice.entity.Application;
import com.jobportal.applicationservice.entity.ApplicationStatus;
import com.jobportal.applicationservice.entity.ApplicationStatusHistory;
import com.jobportal.applicationservice.exception.AlreadyAppliedException;
import com.jobportal.applicationservice.exception.BadRequestException;
import com.jobportal.applicationservice.exception.UnauthorizedActionException;
import com.jobportal.applicationservice.repository.ApplicationRepository;
import com.jobportal.commonsecurity.security.JwtUserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private JobLookupService jobLookupService;
    @Mock
    private ResumeLookupService resumeLookupService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    @Test
    void applyCreatesApplicationAndPublishesEvent() {
        ApplyRequest request = ApplyRequest.builder()
                .jobId(10L)
                .resumeId(20L)
                .coverLetter("Interested")
                .build();

        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "seeker@example.com", "JOB_SEEKER");
        JobSummaryResponse job = JobSummaryResponse.builder()
                .id(10L)
                .title("Java Developer")
                .status("OPEN")
                .recruiterId(99L)
                .build();

        ResumeSummaryResponse resume = ResumeSummaryResponse.builder()
                .id(20L)
                .userId(5L)
                .fileName("resume.pdf")
                .build();

        when(jobLookupService.getJob(10L)).thenReturn(job);
        when(resumeLookupService.getAccessibleResume(20L)).thenReturn(resume);
        when(applicationRepository.existsByJobIdAndCandidateId(10L, 5L)).thenReturn(false);
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> {
            Application application = invocation.getArgument(0);
            application.setId(100L);
            return application;
        });

        ApplicationResponse response = applicationService.apply(request, principal);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(response.getCandidateId()).isEqualTo(5L);
        verify(eventPublisher).publishJobApplied(any());
    }

    @Test
    void applyRejectsClosedJobs() {
        ApplyRequest request = ApplyRequest.builder().jobId(10L).resumeId(20L).build();
        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "seeker@example.com", "JOB_SEEKER");

        when(jobLookupService.getJob(10L)).thenReturn(
                JobSummaryResponse.builder().id(10L).status("CLOSED").recruiterId(99L).build()
        );

        assertThatThrownBy(() -> applicationService.apply(request, principal))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only OPEN jobs can accept applications");
    }

    @Test
    void applyRejectsResumeOwnedByAnotherUser() {
        ApplyRequest request = ApplyRequest.builder().jobId(10L).resumeId(20L).build();
        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "seeker@example.com", "JOB_SEEKER");

        when(jobLookupService.getJob(10L)).thenReturn(
                JobSummaryResponse.builder().id(10L).status("OPEN").recruiterId(99L).build()
        );
        when(resumeLookupService.getAccessibleResume(20L)).thenReturn(
                ResumeSummaryResponse.builder().id(20L).userId(999L).fileName("resume.pdf").build()
        );

        assertThatThrownBy(() -> applicationService.apply(request, principal))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Resume does not belong");
    }

    @Test
    void applyRejectsDuplicateApplication() {
        ApplyRequest request = ApplyRequest.builder().jobId(10L).resumeId(20L).build();
        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "seeker@example.com", "JOB_SEEKER");

        when(jobLookupService.getJob(10L)).thenReturn(
                JobSummaryResponse.builder().id(10L).status("OPEN").recruiterId(99L).build()
        );
        when(resumeLookupService.getAccessibleResume(20L)).thenReturn(
                ResumeSummaryResponse.builder().id(20L).userId(5L).fileName("resume.pdf").build()
        );
        when(applicationRepository.existsByJobIdAndCandidateId(10L, 5L)).thenReturn(true);

        assertThatThrownBy(() -> applicationService.apply(request, principal))
                .isInstanceOf(AlreadyAppliedException.class);
    }

    @Test
    void getByIdRejectsUnrelatedUser() {
        Application application = Application.builder()
                .id(200L)
                .jobId(10L)
                .candidateId(5L)
                .resumeId(20L)
                .status(ApplicationStatus.APPLIED)
                .build();

        JwtUserPrincipal principal = new JwtUserPrincipal(77L, "other@example.com", "JOB_SEEKER");

        when(applicationRepository.findById(200L)).thenReturn(Optional.of(application));
        when(jobLookupService.getJob(10L)).thenReturn(
                JobSummaryResponse.builder().id(10L).title("Java Developer").status("OPEN").recruiterId(99L).build()
        );

        assertThatThrownBy(() -> applicationService.getById(200L, principal))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void updateStatusAllowsOwningRecruiterAndPublishesEvent() {
        Application application = Application.builder()
                .id(200L)
                .jobId(10L)
                .candidateId(5L)
                .resumeId(20L)
                .status(ApplicationStatus.APPLIED)
                .build();

        JwtUserPrincipal recruiter = new JwtUserPrincipal(99L, "recruiter@example.com", "RECRUITER");
        StatusUpdateRequest request = new StatusUpdateRequest(ApplicationStatus.SHORTLISTED, "Good profile");
        JobSummaryResponse job = JobSummaryResponse.builder()
                .id(10L)
                .title("Java Developer")
                .status("OPEN")
                .recruiterId(99L)
                .build();

        when(applicationRepository.findById(200L)).thenReturn(Optional.of(application));
        when(jobLookupService.getJob(10L)).thenReturn(job);
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApplicationResponse response = applicationService.updateStatus(200L, request, recruiter);

        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.SHORTLISTED);
        verify(eventPublisher).publishStatusChanged(any());
    }

    @Test
    void getByCandidateIdRejectsOtherUsers() {
        JwtUserPrincipal principal = new JwtUserPrincipal(7L, "other@example.com", "JOB_SEEKER");

        assertThatThrownBy(() -> applicationService.getByCandidateId(5L, principal))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    void getHistoryReturnsMappedStatusHistory() {
        Application application = Application.builder()
                .id(300L)
                .jobId(10L)
                .candidateId(5L)
                .resumeId(20L)
                .status(ApplicationStatus.SHORTLISTED)
                .build();

        application.addHistory(ApplicationStatusHistory.builder()
                .oldStatus("APPLIED")
                .newStatus("SHORTLISTED")
                .changedBy(99L)
                .notes("Strong profile")
                .build());

        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "seeker@example.com", "JOB_SEEKER");

        when(applicationRepository.findById(300L)).thenReturn(Optional.of(application));
        when(jobLookupService.getJob(10L)).thenReturn(
                JobSummaryResponse.builder().id(10L).title("Java Developer").status("OPEN").recruiterId(99L).build()
        );

        List<ApplicationStatusHistoryResponse> history = applicationService.getHistory(300L, principal);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getOldStatus()).isEqualTo("APPLIED");
        assertThat(history.get(0).getNewStatus()).isEqualTo("SHORTLISTED");
        assertThat(history.get(0).getNotes()).isEqualTo("Strong profile");
    }


    @Test
    void getByIdReturnsApplicationForCandidateOwner() {
        Application application = Application.builder()
                .id(201L)
                .jobId(10L)
                .candidateId(5L)
                .resumeId(20L)
                .status(ApplicationStatus.APPLIED)
                .coverLetter("Interested")
                .build();

        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "seeker@example.com", "JOB_SEEKER");
        when(applicationRepository.findById(201L)).thenReturn(Optional.of(application));
        when(jobLookupService.getJob(10L)).thenReturn(
                JobSummaryResponse.builder().id(10L).title("Java Developer").status("OPEN").recruiterId(99L).build()
        );

        ApplicationResponse response = applicationService.getById(201L, principal);

        assertThat(response.getId()).isEqualTo(201L);
        assertThat(response.getJobTitle()).isEqualTo("Java Developer");
    }

    @Test
    void getByJobIdReturnsApplicationsForOwningRecruiter() {
        JwtUserPrincipal recruiter = new JwtUserPrincipal(99L, "recruiter@example.com", "RECRUITER");
        Application application = Application.builder()
                .id(301L)
                .jobId(10L)
                .candidateId(5L)
                .resumeId(20L)
                .status(ApplicationStatus.APPLIED)
                .build();
        when(jobLookupService.getJob(10L)).thenReturn(
                JobSummaryResponse.builder().id(10L).title("Java Developer").status("OPEN").recruiterId(99L).build()
        );
        when(applicationRepository.findByJobIdOrderByAppliedAtDesc(10L)).thenReturn(List.of(application));

        List<ApplicationResponse> responses = applicationService.getByJobId(10L, recruiter);

        assertThat(responses).singleElement()
                .extracting(ApplicationResponse::getId, ApplicationResponse::getCandidateId)
                .containsExactly(301L, 5L);
    }

    @Test
    void getByCandidateIdAllowsAdminToFetchApplications() {
        JwtUserPrincipal admin = new JwtUserPrincipal(1L, "admin@example.com", "ADMIN");
        Application application = Application.builder()
                .id(401L)
                .jobId(10L)
                .candidateId(5L)
                .resumeId(20L)
                .status(ApplicationStatus.APPLIED)
                .build();
        when(applicationRepository.findByCandidateIdOrderByAppliedAtDesc(5L)).thenReturn(List.of(application));
        when(jobLookupService.getJob(10L)).thenReturn(
                JobSummaryResponse.builder().id(10L).title("Java Developer").status("OPEN").recruiterId(99L).build()
        );

        List<ApplicationResponse> responses = applicationService.getByCandidateId(5L, admin);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getJobTitle()).isEqualTo("Java Developer");
    }

    @Test
    void withdrawMarksApplicationAsWithdrawn() {
        Application application = Application.builder()
                .id(500L)
                .jobId(10L)
                .candidateId(5L)
                .resumeId(20L)
                .status(ApplicationStatus.APPLIED)
                .build();
        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "seeker@example.com", "JOB_SEEKER");
        when(applicationRepository.findById(500L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jobLookupService.getJob(10L)).thenReturn(
                JobSummaryResponse.builder().id(10L).title("Java Developer").status("OPEN").recruiterId(99L).build()
        );

        ApplicationResponse response = applicationService.withdraw(500L, principal);

        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.WITHDRAWN);
        assertThat(application.getStatusHistory()).hasSize(1);
    }


    @Test
    void applyRejectsUnauthorizedRole() {
        ApplyRequest request = ApplyRequest.builder().jobId(10L).resumeId(20L).build();
        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "recruiter@example.com", "RECRUITER");

        assertThatThrownBy(() -> applicationService.apply(request, principal))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessageContaining("Only job seekers can apply");
    }

    @Test
    void updateStatusRejectsRecruiterWhoDoesNotOwnJob() {
        Application application = Application.builder()
                .id(600L)
                .jobId(10L)
                .candidateId(5L)
                .resumeId(20L)
                .status(ApplicationStatus.APPLIED)
                .build();
        JwtUserPrincipal recruiter = new JwtUserPrincipal(55L, "other-recruiter@example.com", "RECRUITER");
        StatusUpdateRequest request = new StatusUpdateRequest(ApplicationStatus.SHORTLISTED, "Good profile");
        when(applicationRepository.findById(600L)).thenReturn(Optional.of(application));
        when(jobLookupService.getJob(10L)).thenReturn(
                JobSummaryResponse.builder().id(10L).title("Java Developer").status("OPEN").recruiterId(99L).build()
        );

        assertThatThrownBy(() -> applicationService.updateStatus(600L, request, recruiter))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessageContaining("owning recruiter");
    }

    @Test
    void withdrawRejectsDifferentCandidate() {
        Application application = Application.builder()
                .id(700L)
                .jobId(10L)
                .candidateId(5L)
                .resumeId(20L)
                .status(ApplicationStatus.APPLIED)
                .build();
        JwtUserPrincipal principal = new JwtUserPrincipal(9L, "other@example.com", "JOB_SEEKER");
        when(applicationRepository.findById(700L)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> applicationService.withdraw(700L, principal))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessageContaining("withdraw only your own application");
    }
}