package com.jobportal.notificationservice;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

class NotificationServiceApplicationTest {

    @Test
    void classEnablesFeignClients() {
        assertThat(NotificationServiceApplication.class.isAnnotationPresent(EnableFeignClients.class)).isTrue();
    }

    @Test
    void mainDelegatesToSpringApplicationRun() {
        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            String[] args = {"--test=true"};
            NotificationServiceApplication.main(args);
            springApplication.verify(() -> SpringApplication.run(NotificationServiceApplication.class, args));
        }
    }
}