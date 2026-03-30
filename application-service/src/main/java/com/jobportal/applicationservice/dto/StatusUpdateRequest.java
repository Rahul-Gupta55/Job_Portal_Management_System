package com.jobportal.applicationservice.dto;

import com.jobportal.applicationservice.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StatusUpdateRequest {
    @NotNull(message = "Status is required")
    private ApplicationStatus status;

    private String notes;
}
