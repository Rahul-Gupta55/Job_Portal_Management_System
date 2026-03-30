package com.jobportal.userservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    @Test
    void openApiIncludesMetadataAndBearerSecurityScheme() {
        OpenAPI openAPI = new OpenApiConfig().openAPI();

        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Job Portal - User Service");
        assertThat(openAPI.getServers()).hasSize(1);
        assertThat(openAPI.getServers().get(0).getUrl()).isEqualTo("/");
        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
        assertThat(openAPI.getSecurity()).hasSize(1);
    }
}