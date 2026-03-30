package com.jobportal.gateway;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class ApiGatewayApplicationTest {

    @Test
    void mainDelegatesToSpringApplicationRun() {
        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            String[] args = {"--test=true"};
            ApiGatewayApplication.main(args);
            springApplication.verify(() -> SpringApplication.run(ApiGatewayApplication.class, args));
        }
    }
}
