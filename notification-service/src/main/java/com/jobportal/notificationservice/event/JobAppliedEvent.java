package com.jobportal.notificationservice.event;

import lombok.*;

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
    private java.time.LocalDateTime appliedAt;
}