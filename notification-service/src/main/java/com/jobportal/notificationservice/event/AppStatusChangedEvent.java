package com.jobportal.notificationservice.event;

import lombok.*;

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
    private String newStatus;
    private String notes;
    private java.time.LocalDateTime changedAt;
}
