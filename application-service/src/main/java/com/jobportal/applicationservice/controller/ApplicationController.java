package com.jobportal.applicationservice.controller;

import com.jobportal.applicationservice.dto.ApiResponse;
import com.jobportal.applicationservice.dto.ApplicationResponse;
import com.jobportal.applicationservice.dto.ApplicationStatusHistoryResponse;
import com.jobportal.applicationservice.dto.ApplyRequest;
import com.jobportal.applicationservice.dto.StatusUpdateRequest;
import com.jobportal.applicationservice.service.ApplicationService;
import com.jobportal.commonsecurity.security.JwtUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@Tag(name = "Application Service", description = "Simple application APIs")
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    @Operation(summary = "Apply for a job")
    public ResponseEntity<ApiResponse<ApplicationResponse>> apply(@Valid @RequestBody ApplyRequest request,
                                                                  @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("Applied successfully", applicationService.apply(request, principal)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get application by id")
    public ResponseEntity<ApiResponse<ApplicationResponse>> getById(@PathVariable Long id,
                                                                    @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.of("Application fetched successfully", applicationService.getById(id, principal)));
    }

    @GetMapping("/job/{jobId}")
    @Operation(summary = "Get applications by job")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getByJobId(@PathVariable Long jobId,
                                                                             @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.of("Applications fetched successfully", applicationService.getByJobId(jobId, principal)));
    }

    @GetMapping("/candidate/{candidateId}")
    @Operation(summary = "Get applications by candidate")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getByCandidate(@PathVariable Long candidateId,
                                                                                 @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.of("Applications fetched successfully", applicationService.getByCandidateId(candidateId, principal)));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update application status")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateStatus(@PathVariable Long id,
                                                                         @Valid @RequestBody StatusUpdateRequest request,
                                                                         @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.of("Application status updated successfully", applicationService.updateStatus(id, request, principal)));
    }

    @PutMapping("/{id}/withdraw")
    @Operation(summary = "Withdraw application")
    public ResponseEntity<ApiResponse<ApplicationResponse>> withdraw(@PathVariable Long id,
                                                                     @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.of("Application withdrawn successfully", applicationService.withdraw(id, principal)));
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Get application status history")
    public ResponseEntity<ApiResponse<List<ApplicationStatusHistoryResponse>>> getHistory(@PathVariable Long id,
                                                                                          @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.of("Application history fetched successfully", applicationService.getHistory(id, principal)));
    }
}
