package com.jobportal.applicationservice.event;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobAppliedEvent {
    private Long applicationId;
    private Long jobId;
    private Long candidateId;
    private Long recruiterId;
    private String jobTitle;
    private LocalDateTime appliedAt;
}