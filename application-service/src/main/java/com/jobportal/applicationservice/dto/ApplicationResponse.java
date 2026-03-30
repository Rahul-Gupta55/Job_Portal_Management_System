package com.jobportal.applicationservice.dto;

import com.jobportal.applicationservice.entity.ApplicationStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationResponse {
    private Long id;
    private Long jobId;
    private String jobTitle;
    private Long candidateId;
    private Long resumeId;
    private ApplicationStatus status;
    private String coverLetter;
    private LocalDateTime appliedAt;
}
