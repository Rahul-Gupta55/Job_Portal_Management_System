package com.jobportal.notificationservice.service;

import com.jobportal.notificationservice.dto.InternalEmailRequest;

public interface EmailNotificationService {
    void sendEmail(InternalEmailRequest request);
}