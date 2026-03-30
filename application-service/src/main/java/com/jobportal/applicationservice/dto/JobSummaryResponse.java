package com.jobportal.applicationservice.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobSummaryResponse {
    private Long id;
    private String title;
    private String status;
    private Long recruiterId;
}
