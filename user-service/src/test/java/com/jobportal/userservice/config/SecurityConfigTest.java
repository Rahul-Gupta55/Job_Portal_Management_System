package com.jobportal.userservice.config;

import com.jobportal.commonsecurity.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private AuthenticationConfiguration authenticationConfiguration;

    @Mock
    private AuthenticationManager authenticationManager;

    @Test
    void passwordEncoderProducesDifferentEncodedValueAndMatchesRawPassword() {
        SecurityConfig securityConfig = new SecurityConfig(jwtAuthenticationFilter);
        var encoder = securityConfig.passwordEncoder();

        String encoded = encoder.encode("Password1");
        assertThat(encoded).isNotEqualTo("Password1");
        assertThat(encoder.matches("Password1", encoded)).isTrue();
    }

    @Test
    void authenticationManagerDelegatesToAuthenticationConfiguration() throws Exception {
        SecurityConfig securityConfig = new SecurityConfig(jwtAuthenticationFilter);
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);

        AuthenticationManager result = securityConfig.authenticationManager(authenticationConfiguration);

        assertThat(result).isSameAs(authenticationManager);
    }


    @Test
    void securityFilterChainAddsJwtFilterAndBuildsChain() throws Exception {
        SecurityConfig securityConfig = new SecurityConfig(jwtAuthenticationFilter);
        org.springframework.security.config.annotation.web.builders.HttpSecurity http =
                org.mockito.Mockito.mock(org.springframework.security.config.annotation.web.builders.HttpSecurity.class, org.mockito.Mockito.RETURNS_SELF);
        org.springframework.security.web.SecurityFilterChain expectedChain = org.mockito.Mockito.mock(org.springframework.security.web.SecurityFilterChain.class);
        org.mockito.Mockito.doReturn(expectedChain).when(http).build();

        var result = securityConfig.securityFilterChain(http);

        assertThat(result).isSameAs(expectedChain);
        org.mockito.Mockito.verify(http).addFilterBefore(jwtAuthenticationFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
    }
}
