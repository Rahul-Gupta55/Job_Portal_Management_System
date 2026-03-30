package com.jobportal.jobservice.event;

import lombok.*;

import java.time.LocalDateTime;

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
    private LocalDateTime createdAt;
}
