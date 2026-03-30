package com.jobportal.userservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private Long userId;
    private String bio;
    private String skills;
    private Integer experienceYrs;
    private String currentCompany;
    private String location;
    private String linkedinUrl;
    private String githubUrl;
    private LocalDateTime updatedAt;
}
