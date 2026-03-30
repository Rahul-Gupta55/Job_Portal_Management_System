package com.jobportal.gateway.config;

import com.jobportal.commonsecurity.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import com.jobportal.commonsecurity.config.CommonSecurityConfiguration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@Import(CommonSecurityConfiguration.class)
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // 1. Public Endpoints (No Auth Required)
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/actuator/health",
                                "/actuator/info",
                                "/api/users/register",
                                "/api/users/login",
                                "/api/users/refresh-token",
                                "/api/users/forgot-password/**",
                                "/api/search/**"
                        ).permitAll()

                        // 2. Job Management (Specific Roles)
                        // Note: GET /api/jobs/recruiter/** must come BEFORE GET /api/jobs/**
                        .requestMatchers(HttpMethod.GET, "/api/jobs/recruiter/**").hasAnyRole("RECRUITER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/jobs/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/jobs/**").hasAnyRole("RECRUITER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/jobs/**").hasAnyRole("RECRUITER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/jobs/**").hasAnyRole("RECRUITER", "ADMIN")

                        // 3. Applications
                        .requestMatchers(HttpMethod.POST, "/api/applications").hasAnyRole("JOB_SEEKER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/applications/job/**").hasAnyRole("RECRUITER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/applications/*/status").hasAnyRole("RECRUITER", "ADMIN")

                        // 4. General Authenticated (Resumes, Notifications, User Profile, etc.)
                        .requestMatchers("/api/applications/**").authenticated()
                        .requestMatchers("/api/resumes/**").authenticated()
                        .requestMatchers("/api/notifications/**").authenticated()
                        .requestMatchers("/api/users/**").authenticated()

                        // 5. Catch-all for any other endpoint
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}