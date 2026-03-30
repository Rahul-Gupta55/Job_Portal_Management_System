package com.jobportal.searchservice.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.*;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .servers(java.util.List.of(new Server().url("/"))).info(new Info().title("Job Portal - Search Service").version("1.0.0").description("Job search APIs").contact(new Contact().name("Job Portal Team").email("api@jobportal.com")));
    }
}
