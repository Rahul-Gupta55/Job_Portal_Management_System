package com.jobportal.applicationservice.client;

import com.jobportal.applicationservice.dto.ApiResponse;
import com.jobportal.applicationservice.dto.ResumeSummaryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "resume-service")
public interface ResumeServiceClient {

    @GetMapping("/api/resumes/{id}")
    ApiResponse<ResumeSummaryResponse> getResumeById(@RequestHeader("Authorization") String authorization,
                                                     @PathVariable("id") Long id);
}
