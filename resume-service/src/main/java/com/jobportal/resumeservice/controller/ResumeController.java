package com.jobportal.resumeservice.controller;

import com.jobportal.commonsecurity.security.JwtUserPrincipal;
import com.jobportal.resumeservice.dto.ApiResponse;
import com.jobportal.resumeservice.dto.ResumeResponse;
import com.jobportal.resumeservice.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
@Tag(name = "Resume Service", description = "Resume PDF upload and management APIs")
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload resume PDF")
    public ResponseEntity<ApiResponse<ResumeResponse>> create(@RequestPart("file") MultipartFile file,
                                                              @RequestParam(name = "primary", defaultValue = "false") Boolean primary,
                                                              @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("Resume uploaded successfully", resumeService.create(file, primary, principal)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get resume by id")
    public ResponseEntity<ApiResponse<ResumeResponse>> getById(@PathVariable Long id,
                                                               @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.of("Resume fetched successfully", resumeService.getById(id, principal)));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get resumes by user")
    public ResponseEntity<ApiResponse<List<ResumeResponse>>> getByUserId(@PathVariable Long userId,
                                                                         @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.of("Resumes fetched successfully", resumeService.getByUserId(userId, principal)));
    }

    @PutMapping("/{id}/primary")
    @Operation(summary = "Set resume as primary")
    public ResponseEntity<ApiResponse<ResumeResponse>> setPrimary(@PathVariable Long id,
                                                                  @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.of("Primary resume updated successfully", resumeService.setPrimary(id, principal)));
    }

    @GetMapping("/{id}/file")
    @Operation(summary = "Download resume PDF")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id,
                                                 @AuthenticationPrincipal JwtUserPrincipal principal) {
        ResumeResponse resume = resumeService.getById(id, principal);
        Resource resource = resumeService.downloadFile(id, principal);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(resume.getFileName()).build().toString())
                .body(resource);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a resume")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                    @AuthenticationPrincipal JwtUserPrincipal principal) {
        resumeService.delete(id, principal);
        return ResponseEntity.ok(ApiResponse.of("Resume deleted successfully", null));
    }
}
