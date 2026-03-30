package com.jobportal.resumeservice.config;

import com.jobportal.commonsecurity.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void securityFilterChainAddsJwtFilterAndBuildsChain() throws Exception {
        SecurityConfig securityConfig = new SecurityConfig(jwtAuthenticationFilter);
        HttpSecurity http = mock(HttpSecurity.class, RETURNS_SELF);
        SecurityFilterChain expectedChain = mock(SecurityFilterChain.class);
        doReturn(expectedChain).when(http).build();

        SecurityFilterChain chain = securityConfig.securityFilterChain(http);

        assertThat(chain).isSameAs(expectedChain);
        verify(http).addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    }
}
