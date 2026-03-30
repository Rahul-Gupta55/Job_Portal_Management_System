package com.jobportal.userservice.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {

    @JsonAlias("accessToken")
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
