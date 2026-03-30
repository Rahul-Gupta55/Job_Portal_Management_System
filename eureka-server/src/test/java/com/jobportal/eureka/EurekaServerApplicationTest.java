package com.jobportal.eureka;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

class EurekaServerApplicationTest {

    @Test
    void classIsAnnotatedAsEurekaServer() {
        assertThat(EurekaServerApplication.class.isAnnotationPresent(EnableEurekaServer.class)).isTrue();
    }

    @Test
    void mainDelegatesToSpringApplicationRun() {
        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            String[] args = {"--test=true"};
            EurekaServerApplication.main(args);
            springApplication.verify(() -> SpringApplication.run(EurekaServerApplication.class, args));
        }
    }
}
