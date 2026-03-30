package com.jobportal.jobservice.dto;

import com.jobportal.jobservice.entity.JobStatus;
import com.jobportal.jobservice.entity.JobType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobResponse {
    private Long id;
    private String title;
    private String company;
    private String location;
    private String salaryRange;
    private String description;
    private String requirements;
    private JobType jobType;
    private JobStatus status;
    private Long recruiterId;
    private List<String> skills;
    private Integer experienceMin;
    private Integer experienceMax;
    private LocalDate deadline;
    private LocalDateTime createdAt;
}
