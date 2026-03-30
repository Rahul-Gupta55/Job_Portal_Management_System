package com.jobportal.jobservice.controller;

import com.jobportal.commonsecurity.security.JwtUserPrincipal;
import com.jobportal.jobservice.dto.ApiResponse;
import com.jobportal.jobservice.dto.CreateJobRequest;
import com.jobportal.jobservice.dto.JobResponse;
import com.jobportal.jobservice.dto.UpdateJobRequest;
import com.jobportal.jobservice.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@Tag(name = "Job Service", description = "Job creation and listing APIs")
public class JobController {

    private final JobService jobService;

    @PostMapping
    @Operation(summary = "Create a new job")
    public ResponseEntity<ApiResponse<JobResponse>> createJob(@Valid @RequestBody CreateJobRequest request,
                                                              @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("Job created successfully", jobService.createJob(request, principal)));
    }

    @GetMapping
    @Operation(summary = "Get all open jobs")
    public ResponseEntity<ApiResponse<Page<JobResponse>>> getAllJobs(@RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.of("Jobs fetched successfully", jobService.getAllOpenJobs(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get job by id")
    public ResponseEntity<ApiResponse<JobResponse>> getJobById(@PathVariable Long id,
                                                               @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.of("Job fetched successfully", jobService.getJobById(id, principal)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a job")
    public ResponseEntity<ApiResponse<JobResponse>> updateJob(@PathVariable Long id,
                                                              @Valid @RequestBody UpdateJobRequest request,
                                                              @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.of("Job updated successfully", jobService.updateJob(id, request, principal)));
    }

    @PutMapping("/{id}/close")
    @Operation(summary = "Close a job")
    public ResponseEntity<ApiResponse<JobResponse>> closeJob(@PathVariable Long id,
                                                             @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.of("Job closed successfully", jobService.closeJob(id, principal)));
    }

    @PutMapping("/{id}/reopen")
    @Operation(summary = "Reopen a closed job")
    public ResponseEntity<ApiResponse<JobResponse>> reopenJob(@PathVariable Long id,
                                                              @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.of("Job reopened successfully", jobService.reopenJob(id, principal)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a draft job")
    public ResponseEntity<ApiResponse<Void>> deleteDraftJob(@PathVariable Long id,
                                                            @AuthenticationPrincipal JwtUserPrincipal principal) {
        jobService.deleteDraftJob(id, principal);
        return ResponseEntity.ok(ApiResponse.of("Draft job deleted successfully", null));
    }

    @GetMapping("/recruiter/{recruiterId}")
    @Operation(summary = "Get jobs by recruiter")
    public ResponseEntity<ApiResponse<List<JobResponse>>> getJobsByRecruiter(@PathVariable Long recruiterId,
                                                                             @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.of("Recruiter jobs fetched successfully", jobService.getJobsByRecruiter(recruiterId, principal)));
    }

    @GetMapping("/{id}/skills")
    @Operation(summary = "Get skills required for a job")
    public ResponseEntity<ApiResponse<List<String>>> getSkills(@PathVariable Long id,
                                                               @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.of("Job skills fetched successfully", jobService.getSkills(id, principal)));
    }
}
