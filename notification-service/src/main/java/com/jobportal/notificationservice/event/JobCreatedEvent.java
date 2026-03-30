package com.jobportal.notificationservice.event;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobCreatedEvent {
    private Long jobId;
    private String title;
    private String company;
    private Long recruiterId;
    private java.time.LocalDateTime createdAt;
}
