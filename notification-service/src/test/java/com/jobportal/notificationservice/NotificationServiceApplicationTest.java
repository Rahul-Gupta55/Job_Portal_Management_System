package com.jobportal.notificationservice;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class NotificationServiceApplicationTest {

    @Test
    void mainDelegatesToSpringApplicationRun() {
        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            String[] args = {"--test=true"};
            NotificationServiceApplication.main(args);
            springApplication.verify(() -> SpringApplication.run(NotificationServiceApplication.class, args));
        }
    }
}
