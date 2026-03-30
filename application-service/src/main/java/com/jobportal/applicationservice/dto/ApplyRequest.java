package com.jobportal.applicationservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyRequest {
    @NotNull(message = "Job id is required")
    private Long jobId;

    @NotNull(message = "Resume id is required")
    private Long resumeId;

    private String coverLetter;
}
