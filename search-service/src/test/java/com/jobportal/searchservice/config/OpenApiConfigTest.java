package com.jobportal.searchservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    @Test
    void openApiContainsExpectedMetadata() {
        OpenAPI openAPI = new OpenApiConfig().openAPI();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Job Portal - Search Service");
        assertThat(openAPI.getServers()).hasSize(1);
    }
}
