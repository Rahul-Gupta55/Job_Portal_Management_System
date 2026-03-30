package com.jobportal.userservice;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

class UserServiceApplicationTest {

    @Test
    void mainDelegatesToSpringApplicationRun() {
        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            String[] args = {"--test=true"};
            UserServiceApplication.main(args);
            springApplication.verify(() -> SpringApplication.run(UserServiceApplication.class, args));
        }
    }

    @Test
    void classEnablesFeignClients() {
        assertThat(UserServiceApplication.class.isAnnotationPresent(EnableFeignClients.class)).isTrue();
    }
}