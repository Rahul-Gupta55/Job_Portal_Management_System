package com.jobportal.notificationservice.event;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobClosedEvent {
    private Long jobId;
    private Long recruiterId;
    private java.time.LocalDateTime closedAt;
}
