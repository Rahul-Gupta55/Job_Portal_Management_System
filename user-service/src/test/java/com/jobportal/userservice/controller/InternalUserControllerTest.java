package com.jobportal.userservice.controller;

import com.jobportal.userservice.dto.InternalUserContactResponse;
import com.jobportal.userservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalUserControllerTest {

    @Mock
    private UserService userService;

    private InternalUserController internalUserController;

    @BeforeEach
    void setUp() {
        internalUserController = new InternalUserController(userService);
        ReflectionTestUtils.setField(internalUserController, "internalApiKey", "jobportal-internal-key");
    }

    @Test
    void getUserContactReturnsInternalContactWhenApiKeyMatches() {
        InternalUserContactResponse response = InternalUserContactResponse.builder()
                .id(5L)
                .name("Alice")
                .email("alice@example.com")
                .role("JOB_SEEKER")
                .active(true)
                .build();
        when(userService.getInternalUserContact(5L)).thenReturn(response);

        var result = internalUserController.getUserContact("jobportal-internal-key", 5L);

        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getData().getEmail()).isEqualTo("alice@example.com");
        verify(userService).getInternalUserContact(5L);
    }

    @Test
    void getUserContactRejectsWrongApiKey() {
        assertThatThrownBy(() -> internalUserController.getUserContact("wrong-key", 5L))
                .isInstanceOf(ResponseStatusException.class);
    }
}