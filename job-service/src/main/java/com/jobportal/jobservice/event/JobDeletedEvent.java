package com.jobportal.jobservice.event;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobDeletedEvent {
    private Long jobId;
}
