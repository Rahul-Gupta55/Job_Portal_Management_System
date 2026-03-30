package com.jobportal.userservice.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalEmailRequest {
    private Long recipientId;
    private String to;
    private String subject;
    private String body;
    private String eventType;
    private Long referenceId;
}