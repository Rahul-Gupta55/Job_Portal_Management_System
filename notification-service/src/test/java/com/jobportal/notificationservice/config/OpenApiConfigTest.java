package com.jobportal.notificationservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    @Test
    void openApiContainsMetadataAndSecurityScheme() {
        OpenAPI openAPI = new OpenApiConfig().openAPI();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Job Portal - Notification Service");
        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
        assertThat(openAPI.getServers()).hasSize(1);
    }
}
