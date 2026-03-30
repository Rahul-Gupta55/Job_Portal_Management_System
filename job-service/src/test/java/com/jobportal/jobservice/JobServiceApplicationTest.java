package com.jobportal.jobservice;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class JobServiceApplicationTest {

    @Test
    void mainDelegatesToSpringApplicationRun() {
        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            String[] args = {"--test=true"};
            JobServiceApplication.main(args);
            springApplication.verify(() -> SpringApplication.run(JobServiceApplication.class, args));
        }
    }
}
