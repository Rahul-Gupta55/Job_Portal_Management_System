package com.jobportal.notificationservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalEmailRequest {

    @NotNull(message = "Recipient id is required")
    private Long recipientId;

    @NotBlank(message = "Destination email is required")
    @Email(message = "Invalid destination email")
    private String to;

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Body is required")
    private String body;

    private String eventType;
    private Long referenceId;
}