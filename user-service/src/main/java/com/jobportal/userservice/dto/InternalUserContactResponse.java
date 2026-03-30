package com.jobportal.userservice.dto;

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
public class InternalUserContactResponse {
    private Long id;
    private String name;
    private String email;
    private String role;
    private boolean active;
}