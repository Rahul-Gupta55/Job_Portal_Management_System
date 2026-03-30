package com.jobportal.resumeservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeResponse {
    private Long id;
    private Long userId;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private boolean primary;
    private LocalDateTime createdAt;
}
