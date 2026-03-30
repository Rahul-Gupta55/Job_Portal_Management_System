package com.jobportal.notificationservice.service;

import org.springframework.stereotype.Service;

@Service
public class EmailTemplateService {

    public String buildProfessionalPlainTextEmail(String subject, String body) {
        String normalizedSubject = clean(subject);
        String normalizedBody = defaultString(body).replace("\r\n", "\n").trim();

        return """
                Job Portal
                ========================================
                Subject: %s
                ========================================

                %s

                ----------------------------------------
                This is an automated message from Job Portal.
                Please do not reply to this email.

                Regards,
                Job Portal Team
                """.formatted(normalizedSubject, normalizedBody);
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return "Notification from Job Portal";
        }
        return value.trim();
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}