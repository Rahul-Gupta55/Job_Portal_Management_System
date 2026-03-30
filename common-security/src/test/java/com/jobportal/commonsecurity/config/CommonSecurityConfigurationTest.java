package com.jobportal.commonsecurity.config;

import com.jobportal.commonsecurity.security.JwtAuthenticationFilter;
import com.jobportal.commonsecurity.security.JwtUtil;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CommonSecurityConfigurationTest {

    private final CommonSecurityConfiguration configuration = new CommonSecurityConfiguration();

    @Test
    void jwtUtilBeanCreatesWorkingUtility() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(Base64.getEncoder().encodeToString("12345678901234567890123456789012".getBytes()));
        properties.setExpiration(60_000L);
        properties.setRefreshExpiration(120_000L);

        JwtUtil jwtUtil = configuration.jwtUtil(properties);

        assertThat(jwtUtil.generateAccessToken(1L, "user@example.com", "JOB_SEEKER")).isNotBlank();
    }

    @Test
    void jwtAuthenticationFilterBeanUsesProvidedJwtUtil() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        FilterChain chain = mock(FilterChain.class);
        JwtAuthenticationFilter filter = configuration.jwtAuthenticationFilter(jwtUtil);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token");

        when(jwtUtil.isTokenValid("token")).thenReturn(false);

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        verify(jwtUtil).isTokenValid("token");
        assertThat(filter).isNotNull();
    }
}
