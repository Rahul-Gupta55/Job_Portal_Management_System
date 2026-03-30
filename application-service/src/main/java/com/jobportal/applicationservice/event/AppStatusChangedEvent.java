package com.jobportal.applicationservice.event;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppStatusChangedEvent {
    private Long applicationId;
    private Long jobId;
    private Long candidateId;
    private Long recruiterId;
    private String jobTitle;
    private String newStatus;
    private String notes;
    private LocalDateTime changedAt;
}