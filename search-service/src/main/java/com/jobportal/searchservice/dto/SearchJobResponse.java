package com.jobportal.searchservice.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchJobResponse {
    private Long id;
    private String title;
    private String company;
    private String location;
    private String salaryRange;
    private String jobType;
    private String status;
    private Integer experienceMin;
    private Integer experienceMax;
    private LocalDate deadline;
}
