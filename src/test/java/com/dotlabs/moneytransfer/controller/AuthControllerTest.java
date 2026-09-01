package com.dotlabs.moneytransfer.controller;

import com.dotlabs.moneytransfer.dto.request.LoginRequest;
import com.dotlabs.moneytransfer.dto.request.RegisterRequest;
import com.dotlabs.moneytransfer.dto.response.AuthResponse;
import com.dotlabs.moneytransfer.enums.Role;
import com.dotlabs.moneytransfer.security.CustomUserDetailsService;
import com.dotlabs.moneytransfer.security.JwtTokenProvider;
import com.dotlabs.moneytransfer.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("POST /api/v1/auth/login - Should return 200 and token on valid credentials")
    void testLoginSuccess() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("emmanuel")
                .password("Password123!")
                .build();

        AuthResponse response = AuthResponse.builder()
                .accessToken("mock.jwt.token")
                .tokenType("Bearer")
                .username("emmanuel")
                .role(Role.ROLE_USER)
                .accountNumber("1000000001")
                .expiresInMs(86400000L)
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("mock.jwt.token"))
                .andExpect(jsonPath("$.data.username").value("emmanuel"))
                .andExpect(jsonPath("$.data.accountNumber").value("1000000001"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Should return 201 Created on registration")
    void testRegisterSuccess() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("new@dotlabs.ai")
                .password("Password123!")
                .fullName("New User")
                .build();

        AuthResponse response = AuthResponse.builder()
                .accessToken("new.jwt.token")
                .tokenType("Bearer")
                .username("newuser")
                .role(Role.ROLE_USER)
                .accountNumber("1001234567")
                .expiresInMs(86400000L)
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("newuser"));
    }
}
