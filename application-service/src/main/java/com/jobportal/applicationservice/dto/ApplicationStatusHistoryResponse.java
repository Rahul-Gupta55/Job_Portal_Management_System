package com.jobportal.applicationservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationStatusHistoryResponse {
    private Long id;
    private String oldStatus;
    private String newStatus;
    private Long changedBy;
    private String notes;
    private LocalDateTime changedAt;
}
