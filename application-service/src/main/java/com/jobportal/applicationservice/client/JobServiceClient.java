package com.jobportal.applicationservice.client;

import com.jobportal.applicationservice.dto.ApiResponse;
import com.jobportal.applicationservice.dto.JobSummaryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "job-service")
public interface JobServiceClient {

    @GetMapping("/api/jobs/{id}")
    ApiResponse<JobSummaryResponse> getJobById(@RequestHeader("Authorization") String authorization,
                                               @PathVariable("id") Long id);
}
