package com.jobportal.jobservice.dto;

import com.jobportal.jobservice.entity.JobStatus;
import com.jobportal.jobservice.entity.JobType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateJobRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Company is required")
    private String company;

    @NotBlank(message = "Location is required")
    private String location;

    private String salaryRange;

    @NotBlank(message = "Description is required")
    @Size(min = 50, message = "Description must be at least 50 characters")
    private String description;

    private String requirements;

    @NotNull(message = "Job type is required")
    private JobType jobType;

    private JobStatus status;

    private List<String> skills;

    private Integer experienceMin;

    private Integer experienceMax;

    private LocalDate deadline;
}
