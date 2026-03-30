package com.jobportal.userservice.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileRequest {
    private String bio;
    private String skills;
    private Integer experienceYrs;
    private String currentCompany;
    private String location;
    private String linkedinUrl;
    private String githubUrl;
}
