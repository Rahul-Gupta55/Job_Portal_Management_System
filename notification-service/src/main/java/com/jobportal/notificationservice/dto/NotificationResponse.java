package com.jobportal.notificationservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private Long recipientId;
    private String type;
    private String subject;
    private String body;
    private String status;
    private String eventType;
    private Long referenceId;
    private boolean read;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
}
