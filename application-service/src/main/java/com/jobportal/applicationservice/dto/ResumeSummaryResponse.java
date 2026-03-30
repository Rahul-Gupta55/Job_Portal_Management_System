package com.jobportal.applicationservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeSummaryResponse {
    private Long id;
    private Long userId;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private boolean primary;
    private LocalDateTime createdAt;
}
