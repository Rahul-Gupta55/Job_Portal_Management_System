package com.jobportal.searchservice.event;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobUpsertedEvent {
    private Long jobId;
    private String title;
    private String company;
    private String location;
    private String salaryRange;
    private String jobType;
    private String status;
    private Long recruiterId;
    private Integer experienceMin;
    private Integer experienceMax;
    private LocalDate deadline;
    private LocalDateTime createdAt;
}
