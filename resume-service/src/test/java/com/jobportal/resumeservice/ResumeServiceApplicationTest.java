package com.jobportal.resumeservice;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class ResumeServiceApplicationTest {

    @Test
    void mainDelegatesToSpringApplicationRun() {
        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            String[] args = {"--test=true"};
            ResumeServiceApplication.main(args);
            springApplication.verify(() -> SpringApplication.run(ResumeServiceApplication.class, args));
        }
    }
}
