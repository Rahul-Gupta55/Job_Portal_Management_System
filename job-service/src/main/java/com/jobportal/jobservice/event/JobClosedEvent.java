package com.jobportal.jobservice.event;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobClosedEvent {
    private Long jobId;
    private Long recruiterId;
    private LocalDateTime closedAt;
}
