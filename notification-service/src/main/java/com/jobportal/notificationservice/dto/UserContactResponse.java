package com.jobportal.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContactResponse {
    private Long id;
    private String name;
    private String email;
    private String role;
    private boolean active;
}