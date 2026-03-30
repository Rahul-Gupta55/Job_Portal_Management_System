package com.jobportal.applicationservice.service;

import com.jobportal.applicationservice.dto.*;
import com.jobportal.applicationservice.entity.Application;
import com.jobportal.applicationservice.entity.ApplicationStatus;
import com.jobportal.applicationservice.entity.ApplicationStatusHistory;
import com.jobportal.applicationservice.exception.AlreadyAppliedException;
import com.jobportal.applicationservice.exception.BadRequestException;
import com.jobportal.applicationservice.exception.ResourceNotFoundException;
import com.jobportal.applicationservice.exception.UnauthorizedActionException;
import com.jobportal.applicationservice.repository.ApplicationRepository;
import com.jobportal.commonsecurity.security.JwtUserPrincipal;
import com.jobportal.applicationservice.event.AppStatusChangedEvent;
import com.jobportal.applicationservice.event.JobAppliedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ApplicationServiceImpl implements ApplicationService {

    private static final String ROLE_ADMIN = "ADMIN";

    private final ApplicationRepository applicationRepository;
    private final JobLookupService jobLookupService;
    private final ResumeLookupService resumeLookupService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public ApplicationResponse apply(ApplyRequest request, JwtUserPrincipal principal) {
        if (!"JOB_SEEKER".equals(principal.role()) && !ROLE_ADMIN.equals(principal.role())) {
            log.warn("Application failed. Unauthorized role. userId={}, role={}",
                    principal.userId(), principal.role());
            throw new UnauthorizedActionException("Only job seekers can apply");
        }

        JobSummaryResponse job = jobLookupService.getJob(request.getJobId());
        if (!"OPEN".equals(job.getStatus())) {
            log.warn("Application failed. Job is not open. userId={}, jobId={}, status={}",
                    principal.userId(), request.getJobId(), job.getStatus());
            throw new BadRequestException("Only OPEN jobs can accept applications");
        }

        ResumeSummaryResponse resume = resumeLookupService.getAccessibleResume(request.getResumeId());
        if (!principal.userId().equals(resume.getUserId()) && !ROLE_ADMIN.equals(principal.role())) {
            log.warn("Application failed. Resume does not belong to user. userId={}, resumeId={}",
                    principal.userId(), request.getResumeId());
            throw new BadRequestException("Resume does not belong to the authenticated user");
        }

        if (applicationRepository.existsByJobIdAndCandidateId(request.getJobId(), principal.userId())) {
            log.warn("Application failed. Already applied. userId={}, jobId={}",
                    principal.userId(), request.getJobId());
            throw new AlreadyAppliedException(request.getJobId());
        }

        Application application = Application.builder()
                .jobId(request.getJobId())
                .candidateId(principal.userId())
                .resumeId(request.getResumeId())
                .coverLetter(request.getCoverLetter())
                .status(ApplicationStatus.APPLIED)
                .build();

        application.addHistory(ApplicationStatusHistory.builder()
                .oldStatus(null)
                .newStatus(ApplicationStatus.APPLIED.name())
                .changedBy(principal.userId())
                .notes("Application submitted")
                .build());

        Application saved = applicationRepository.save(application);

        eventPublisher.publishJobApplied(JobAppliedEvent.builder()
                .applicationId(saved.getId())
                .jobId(saved.getJobId())
                .candidateId(saved.getCandidateId())
                .recruiterId(job.getRecruiterId())
                .appliedAt(saved.getAppliedAt())
                .build());

        log.info("Application submitted successfully. applicationId={}, jobId={}, candidateId={}",
                saved.getId(), saved.getJobId(), saved.getCandidateId());

        return toResponse(saved, job);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationResponse getById(Long id, JwtUserPrincipal principal) {
        Application application = findById(id);
        JobSummaryResponse job = jobLookupService.getJob(application.getJobId());
        ensureCanView(application, job, principal);

        log.info("Application fetched successfully. applicationId={}", application.getId());

        return toResponse(application, job);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getByJobId(Long jobId, JwtUserPrincipal principal) {
        JobSummaryResponse job = jobLookupService.getJob(jobId);
        ensureRecruiterOwnerOrAdmin(job, principal);

        List<ApplicationResponse> responses = applicationRepository.findByJobIdOrderByAppliedAtDesc(jobId)
                .stream()
                .map(app -> toResponse(app, job))
                .toList();

        log.info("Applications fetched for job. jobId={}, count={}", jobId, responses.size());

        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getByCandidateId(Long candidateId, JwtUserPrincipal principal) {
        if (!principal.userId().equals(candidateId) && !ROLE_ADMIN.equals(principal.role())) {
            log.warn("Access denied for candidate applications. requesterId={}, candidateId={}",
                    principal.userId(), candidateId);
            throw new UnauthorizedActionException("You can view only your own applications");
        }

        List<ApplicationResponse> responses = applicationRepository.findByCandidateIdOrderByAppliedAtDesc(candidateId)
                .stream()
                .map(app -> toResponse(app, jobLookupService.getJob(app.getJobId())))
                .toList();

        log.info("Applications fetched for candidate. candidateId={}, count={}", candidateId, responses.size());

        return responses;
    }

    @Override
    public ApplicationResponse updateStatus(Long id, StatusUpdateRequest request, JwtUserPrincipal principal) {
        Application application = findById(id);
        JobSummaryResponse job = jobLookupService.getJob(application.getJobId());
        ensureRecruiterOwnerOrAdmin(job, principal);

        ApplicationStatus oldStatus = application.getStatus();
        application.setStatus(request.getStatus());
        application.addHistory(ApplicationStatusHistory.builder()
                .oldStatus(oldStatus == null ? null : oldStatus.name())
                .newStatus(request.getStatus().name())
                .changedBy(principal.userId())
                .notes(request.getNotes())
                .build());

        Application saved = applicationRepository.save(application);

        eventPublisher.publishStatusChanged(AppStatusChangedEvent.builder()
                .applicationId(saved.getId())
                .jobId(saved.getJobId())
                .candidateId(saved.getCandidateId())
                .recruiterId(job.getRecruiterId())
                .newStatus(saved.getStatus().name())
                .notes(request.getNotes())
                .changedAt(LocalDateTime.now())
                .build());

        log.info("Application status updated. applicationId={}, oldStatus={}, newStatus={}",
                saved.getId(), oldStatus, saved.getStatus());

        return toResponse(saved, job);
    }

    @Override
    public ApplicationResponse withdraw(Long id, JwtUserPrincipal principal) {
        Application application = findById(id);
        if (!principal.userId().equals(application.getCandidateId()) && !ROLE_ADMIN.equals(principal.role())) {
            log.warn("Withdraw failed. Unauthorized access. userId={}, applicationId={}",
                    principal.userId(), id);
            throw new UnauthorizedActionException("You can withdraw only your own application");
        }

        ApplicationStatus oldStatus = application.getStatus();
        application.setStatus(ApplicationStatus.WITHDRAWN);
        application.addHistory(ApplicationStatusHistory.builder()
                .oldStatus(oldStatus == null ? null : oldStatus.name())
                .newStatus(ApplicationStatus.WITHDRAWN.name())
                .changedBy(principal.userId())
                .notes("Application withdrawn")
                .build());

        Application saved = applicationRepository.save(application);

        log.info("Application withdrawn successfully. applicationId={}, candidateId={}",
                saved.getId(), saved.getCandidateId());

        return toResponse(saved, jobLookupService.getJob(saved.getJobId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationStatusHistoryResponse> getHistory(Long id, JwtUserPrincipal principal) {
        Application application = findById(id);
        JobSummaryResponse job = jobLookupService.getJob(application.getJobId());
        ensureCanView(application, job, principal);

        List<ApplicationStatusHistoryResponse> history = application.getStatusHistory().stream().map(historyItem -> ApplicationStatusHistoryResponse.builder()
                .id(historyItem.getId())
                .oldStatus(historyItem.getOldStatus())
                .newStatus(historyItem.getNewStatus())
                .changedBy(historyItem.getChangedBy())
                .notes(historyItem.getNotes())
                .changedAt(historyItem.getChangedAt())
                .build()).toList();

        log.info("Application history fetched successfully. applicationId={}, count={}",
                application.getId(), history.size());

        return history;
    }

    private Application findById(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application", id));
    }

    private void ensureRecruiterOwnerOrAdmin(JobSummaryResponse job, JwtUserPrincipal principal) {
        if (!ROLE_ADMIN.equals(principal.role())
                && !("RECRUITER".equals(principal.role()) && principal.userId().equals(job.getRecruiterId()))) {
            log.warn("Access denied. Only owner recruiter or admin allowed. userId={}, role={}",
                    principal.userId(), principal.role());
            throw new UnauthorizedActionException("Only the owning recruiter or admin can perform this action");
        }
    }

    private void ensureCanView(Application application, JobSummaryResponse job, JwtUserPrincipal principal) {
        boolean candidateOwns = principal.userId().equals(application.getCandidateId());
        boolean recruiterOwns = "RECRUITER".equals(principal.role()) && principal.userId().equals(job.getRecruiterId());
        boolean admin = ROLE_ADMIN.equals(principal.role());

        if (!(candidateOwns || recruiterOwns || admin)) {
            log.warn("Access denied for application. userId={}, applicationId={}",
                    principal.userId(), application.getId());
            throw new UnauthorizedActionException("You are not allowed to access this application");
        }
    }

    private ApplicationResponse toResponse(Application application, JobSummaryResponse job) {
        return ApplicationResponse.builder()
                .id(application.getId())
                .jobId(application.getJobId())
                .jobTitle(job.getTitle())
                .candidateId(application.getCandidateId())
                .resumeId(application.getResumeId())
                .status(application.getStatus())
                .coverLetter(application.getCoverLetter())
                .appliedAt(application.getAppliedAt())
                .build();
    }
}
