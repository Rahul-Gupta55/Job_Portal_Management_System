package com.jobportal.jobservice.service;

import com.jobportal.jobservice.dto.CreateJobRequest;
import com.jobportal.jobservice.dto.JobResponse;
import com.jobportal.jobservice.dto.UpdateJobRequest;
import com.jobportal.commonsecurity.security.JwtUserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface JobService {
    JobResponse createJob(CreateJobRequest request, JwtUserPrincipal principal);
    Page<JobResponse> getAllOpenJobs(Pageable pageable);
    JobResponse getJobById(Long id, JwtUserPrincipal principal);
    JobResponse updateJob(Long id, UpdateJobRequest request, JwtUserPrincipal principal);
    JobResponse closeJob(Long id, JwtUserPrincipal principal);
    JobResponse reopenJob(Long id, JwtUserPrincipal principal);
    void deleteDraftJob(Long id, JwtUserPrincipal principal);
    List<JobResponse> getJobsByRecruiter(Long recruiterId, JwtUserPrincipal principal);
    List<String> getSkills(Long id, JwtUserPrincipal principal);
}
