package com.jobportal.commonsecurity.security;

import com.jobportal.commonsecurity.config.JwtProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtSupportTypesTest {

    @Test
    void jwtPropertiesStoresConfiguredValues() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("secret-value");
        properties.setExpiration(111L);
        properties.setRefreshExpiration(222L);

        assertThat(properties.getSecret()).isEqualTo("secret-value");
        assertThat(properties.getExpiration()).isEqualTo(111L);
        assertThat(properties.getRefreshExpiration()).isEqualTo(222L);
    }

    @Test
    void jwtUserPrincipalExposesConstructorValues() {
        JwtUserPrincipal principal = new JwtUserPrincipal(9L, "user@example.com", "ADMIN");

        assertThat(principal.userId()).isEqualTo(9L);
        assertThat(principal.email()).isEqualTo("user@example.com");
        assertThat(principal.role()).isEqualTo("ADMIN");
    }
}
