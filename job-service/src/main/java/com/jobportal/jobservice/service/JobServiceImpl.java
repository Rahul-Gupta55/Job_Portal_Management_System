package com.jobportal.jobservice.service;

import com.jobportal.jobservice.dto.CreateJobRequest;
import com.jobportal.jobservice.dto.JobResponse;
import com.jobportal.jobservice.dto.UpdateJobRequest;
import com.jobportal.jobservice.entity.Job;
import com.jobportal.jobservice.entity.JobSkill;
import com.jobportal.jobservice.entity.JobStatus;
import com.jobportal.jobservice.event.JobClosedEvent;
import com.jobportal.jobservice.event.JobCreatedEvent;
import com.jobportal.jobservice.event.JobDeletedEvent;
import com.jobportal.jobservice.event.JobUpsertedEvent;
import com.jobportal.jobservice.exception.BadRequestException;
import com.jobportal.jobservice.exception.ResourceNotFoundException;
import com.jobportal.jobservice.exception.UnauthorizedActionException;
import com.jobportal.jobservice.repository.JobRepository;
import com.jobportal.commonsecurity.security.JwtUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class JobServiceImpl implements JobService {

    private static final String ROLE_ADMIN = "ADMIN";

    private final JobRepository jobRepository;
    private final JobEventPublisher jobEventPublisher;

    @Override
    public JobResponse createJob(CreateJobRequest request, JwtUserPrincipal principal) {
        ensureRecruiterOrAdmin(principal);
        validateExperienceRange(request.getExperienceMin(), request.getExperienceMax());

        Job job = Job.builder()
                .title(request.getTitle())
                .company(request.getCompany())
                .location(request.getLocation())
                .salaryRange(request.getSalaryRange())
                .description(request.getDescription())
                .requirements(request.getRequirements())
                .jobType(request.getJobType())
                .status(request.getStatus() == null ? JobStatus.OPEN : request.getStatus())
                .recruiterId(principal.userId())
                .experienceMin(request.getExperienceMin())
                .experienceMax(request.getExperienceMax())
                .deadline(request.getDeadline())
                .build();

        applySkills(job, request.getSkills());
        Job saved = jobRepository.save(job);

        if (saved.getStatus() == JobStatus.OPEN) {
            jobEventPublisher.publishJobCreated(JobCreatedEvent.builder()
                    .jobId(saved.getId())
                    .title(saved.getTitle())
                    .company(saved.getCompany())
                    .recruiterId(saved.getRecruiterId())
                    .createdAt(saved.getCreatedAt())
                    .build());
        }
        publishUpsertEvent(saved);

        log.info("event=job_created service=job-service jobId={} recruiterId={} status={} skillCount={}",
                saved.getId(), saved.getRecruiterId(), saved.getStatus(), saved.getSkills().size());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobResponse> getAllOpenJobs(Pageable pageable) {
        Page<JobResponse> jobs = jobRepository.findByStatus(JobStatus.OPEN, pageable).map(this::toResponse);
        log.info("event=job_list_open service=job-service pageNumber={} pageSize={} resultCount={} totalElements={}",
                pageable.getPageNumber(), pageable.getPageSize(), jobs.getNumberOfElements(), jobs.getTotalElements());
        return jobs;
    }

    @Override
    @Transactional(readOnly = true)
    public JobResponse getJobById(Long id, JwtUserPrincipal principal) {
        Job job = findJob(id);
        ensureVisible(job, principal);
        log.info("event=job_fetched service=job-service jobId={} requesterUserId={} requesterRole={} status={}",
                job.getId(), principal == null ? null : principal.userId(), principal == null ? "ANONYMOUS" : principal.role(), job.getStatus());
        return toResponse(job);
    }

    @Override
    public JobResponse updateJob(Long id, UpdateJobRequest request, JwtUserPrincipal principal) {
        Job job = findJob(id);
        ensureOwnerOrAdmin(job, principal);
        validateExperienceRange(request.getExperienceMin(), request.getExperienceMax());

        if (job.getStatus() == JobStatus.CLOSED && request.getStatus() == JobStatus.DRAFT) {
            log.warn("event=job_update_rejected service=job-service reason=invalid_status_transition jobId={} actorUserId={} actorRole={} currentStatus={} requestedStatus={}",
                    id, principal.userId(), principal.role(), job.getStatus(), request.getStatus());
            throw new BadRequestException("Closed job cannot be moved to draft");
        }

        job.setTitle(request.getTitle());
        job.setCompany(request.getCompany());
        job.setLocation(request.getLocation());
        job.setSalaryRange(request.getSalaryRange());
        job.setDescription(request.getDescription());
        job.setRequirements(request.getRequirements());
        job.setJobType(request.getJobType());
        job.setStatus(request.getStatus() == null ? job.getStatus() : request.getStatus());
        job.setExperienceMin(request.getExperienceMin());
        job.setExperienceMax(request.getExperienceMax());
        job.setDeadline(request.getDeadline());
        job.getSkills().clear();
        applySkills(job, request.getSkills());

        Job saved = jobRepository.save(job);
        publishUpsertEvent(saved);
        log.info("event=job_updated service=job-service jobId={} actorUserId={} actorRole={} status={} skillCount={}",
                saved.getId(), principal.userId(), principal.role(), saved.getStatus(), saved.getSkills().size());
        return toResponse(saved);
    }

    @Override
    public JobResponse closeJob(Long id, JwtUserPrincipal principal) {
        Job job = findJob(id);
        ensureOwnerOrAdmin(job, principal);
        job.setStatus(JobStatus.CLOSED);
        Job saved = jobRepository.save(job);
        jobEventPublisher.publishJobClosed(JobClosedEvent.builder()
                .jobId(saved.getId())
                .recruiterId(saved.getRecruiterId())
                .closedAt(LocalDateTime.now())
                .build());
        publishUpsertEvent(saved);
        log.info("event=job_closed service=job-service jobId={} actorUserId={} actorRole={} recruiterId={}",
                saved.getId(), principal.userId(), principal.role(), saved.getRecruiterId());
        return toResponse(saved);
    }

    @Override
    public JobResponse reopenJob(Long id, JwtUserPrincipal principal) {
        Job job = findJob(id);
        ensureOwnerOrAdmin(job, principal);
        job.setStatus(JobStatus.OPEN);
        Job saved = jobRepository.save(job);
        publishUpsertEvent(saved);
        log.info("event=job_reopened service=job-service jobId={} actorUserId={} actorRole={} recruiterId={}",
                saved.getId(), principal.userId(), principal.role(), saved.getRecruiterId());
        return toResponse(saved);
    }

    @Override
    public void deleteDraftJob(Long id, JwtUserPrincipal principal) {
        Job job = findJob(id);
        ensureOwnerOrAdmin(job, principal);
        if (job.getStatus() != JobStatus.DRAFT) {
            log.warn("event=job_delete_rejected service=job-service reason=job_not_draft jobId={} actorUserId={} actorRole={} currentStatus={}",
                    id, principal.userId(), principal.role(), job.getStatus());
            throw new BadRequestException("Only DRAFT jobs can be deleted");
        }
        Long jobId = job.getId();
        jobRepository.delete(job);
        jobEventPublisher.publishJobDeleted(JobDeletedEvent.builder().jobId(jobId).build());
        log.info("event=job_deleted service=job-service jobId={} actorUserId={} actorRole={}",
                jobId, principal.userId(), principal.role());
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobResponse> getJobsByRecruiter(Long recruiterId, JwtUserPrincipal principal) {
        if (principal == null || (!principal.userId().equals(recruiterId) && !ROLE_ADMIN.equals(principal.role()))) {
            log.warn("event=job_list_by_recruiter_rejected service=job-service reason=unauthorized_access requesterUserId={} requesterRole={} recruiterId={}",
                    principal == null ? null : principal.userId(), principal == null ? null : principal.role(), recruiterId);
            throw new UnauthorizedActionException("You can view only your own jobs");
        }
        List<JobResponse> jobs = jobRepository.findByRecruiterIdOrderByCreatedAtDesc(recruiterId).stream().map(this::toResponse).toList();
        log.info("event=job_list_by_recruiter service=job-service recruiterId={} requesterUserId={} requesterRole={} resultCount={}",
                recruiterId, principal.userId(), principal.role(), jobs.size());
        return jobs;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getSkills(Long id, JwtUserPrincipal principal) {
        Job job = findJob(id);
        ensureVisible(job, principal);
        List<String> skills = job.getSkills().stream().map(JobSkill::getSkill).toList();
        log.info("event=job_skills_fetched service=job-service jobId={} requesterUserId={} requesterRole={} skillCount={}",
                job.getId(), principal == null ? null : principal.userId(), principal == null ? "ANONYMOUS" : principal.role(), skills.size());
        return skills;
    }

    private Job findJob(Long id) {
        return jobRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Job", id));
    }

    private void ensureRecruiterOrAdmin(JwtUserPrincipal principal) {
        if (principal == null || (!"RECRUITER".equals(principal.role()) && !ROLE_ADMIN.equals(principal.role()))) {
            log.warn("event=job_action_rejected service=job-service reason=unauthorized_role requesterUserId={} requesterRole={}",
                    principal == null ? null : principal.userId(), principal == null ? null : principal.role());
            throw new UnauthorizedActionException("Only recruiters and admins can manage jobs");
        }
    }

    private void ensureOwnerOrAdmin(Job job, JwtUserPrincipal principal) {
        ensureRecruiterOrAdmin(principal);
        if (!principal.userId().equals(job.getRecruiterId()) && !ROLE_ADMIN.equals(principal.role())) {
            log.warn("event=job_action_rejected service=job-service reason=owner_mismatch requesterUserId={} requesterRole={} recruiterId={} jobId={}",
                    principal.userId(), principal.role(), job.getRecruiterId(), job.getId());
            throw new UnauthorizedActionException("You can manage only your own jobs");
        }
    }

    private void ensureVisible(Job job, JwtUserPrincipal principal) {
        if (job.getStatus() == JobStatus.OPEN) {
            return;
        }

        boolean admin = principal != null && ROLE_ADMIN.equals(principal.role());
        boolean owner = principal != null && principal.userId().equals(job.getRecruiterId())
                && ("RECRUITER".equals(principal.role()) || ROLE_ADMIN.equals(principal.role()));

        if (!(admin || owner)) {
            log.warn("event=job_visibility_rejected service=job-service reason=job_not_visible jobId={} requesterUserId={} requesterRole={} jobStatus={}",
                    job.getId(), principal == null ? null : principal.userId(), principal == null ? null : principal.role(), job.getStatus());
            throw new ResourceNotFoundException("Job", job.getId());
        }
    }

    private void validateExperienceRange(Integer experienceMin, Integer experienceMax) {
        if (experienceMin != null && experienceMax != null && experienceMin > experienceMax) {
            log.warn("event=job_validation_failed service=job-service reason=invalid_experience_range experienceMin={} experienceMax={}",
                    experienceMin, experienceMax);
            throw new BadRequestException("Minimum experience cannot be greater than maximum experience");
        }
    }

    private void applySkills(Job job, List<String> skills) {
        if (skills == null) {
            skills = Collections.emptyList();
            log.warn("event=job_skills_defaulted service=job-service reason=null_skill_list recruiterId={} jobId={}",
                    job.getRecruiterId(), job.getId());
        }
        for (String skillName : skills) {
            if (skillName != null && !skillName.isBlank()) {
                job.addSkill(JobSkill.builder().skill(skillName.trim()).build());
            }
        }
    }

    private void publishUpsertEvent(Job job) {
        jobEventPublisher.publishJobUpserted(JobUpsertedEvent.builder()
                .jobId(job.getId())
                .title(job.getTitle())
                .company(job.getCompany())
                .location(job.getLocation())
                .salaryRange(job.getSalaryRange())
                .jobType(job.getJobType() == null ? null : job.getJobType().name())
                .status(job.getStatus() == null ? null : job.getStatus().name())
                .recruiterId(job.getRecruiterId())
                .experienceMin(job.getExperienceMin())
                .experienceMax(job.getExperienceMax())
                .deadline(job.getDeadline())
                .createdAt(job.getCreatedAt())
                .build());
    }

    private JobResponse toResponse(Job job) {
        return JobResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .company(job.getCompany())
                .location(job.getLocation())
                .salaryRange(job.getSalaryRange())
                .description(job.getDescription())
                .requirements(job.getRequirements())
                .jobType(job.getJobType())
                .status(job.getStatus())
                .recruiterId(job.getRecruiterId())
                .skills(job.getSkills().stream().map(JobSkill::getSkill).toList())
                .experienceMin(job.getExperienceMin())
                .experienceMax(job.getExperienceMax())
                .deadline(job.getDeadline())
                .createdAt(job.getCreatedAt())
                .build();
    }
}
