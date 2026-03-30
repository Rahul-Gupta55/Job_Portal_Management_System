package com.jobportal.configserver;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

class ConfigServerApplicationTest {

    @Test
    void classIsAnnotatedAsConfigServer() {
        assertThat(ConfigServerApplication.class.isAnnotationPresent(EnableConfigServer.class)).isTrue();
    }

    @Test
    void mainDelegatesToSpringApplicationRun() {
        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            String[] args = {"--test=true"};
            ConfigServerApplication.main(args);
            springApplication.verify(() -> SpringApplication.run(ConfigServerApplication.class, args));
        }
    }
}
