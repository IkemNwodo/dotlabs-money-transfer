package com.dotlabs.moneytransfer.service;

import com.dotlabs.moneytransfer.dto.request.LoginRequest;
import com.dotlabs.moneytransfer.dto.request.RegisterRequest;
import com.dotlabs.moneytransfer.dto.response.AuthResponse;
import com.dotlabs.moneytransfer.entity.Account;
import com.dotlabs.moneytransfer.entity.User;
import com.dotlabs.moneytransfer.enums.Role;
import com.dotlabs.moneytransfer.exception.InvalidTransferException;
import com.dotlabs.moneytransfer.repository.AccountRepository;
import com.dotlabs.moneytransfer.repository.UserRepository;
import com.dotlabs.moneytransfer.security.JwtTokenProvider;
import com.dotlabs.moneytransfer.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .username("emmanuel")
                .email("emmanuel@dotlabs.ai")
                .password("hashed_password")
                .role(Role.ROLE_USER)
                .accountNumber("1000000001")
                .build();
    }

    @Test
    @DisplayName("Should successfully authenticate and return JWT token on login")
    void testLoginSuccess() {
        LoginRequest request = LoginRequest.builder()
                .username("emmanuel")
                .password("Password123!")
                .build();

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(sampleUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtTokenProvider.generateToken(auth)).thenReturn("mock.jwt.token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mock.jwt.token");
        assertThat(response.getUsername()).isEqualTo("emmanuel");
        assertThat(response.getAccountNumber()).isEqualTo("1000000001");
        assertThat(response.getRole()).isEqualTo(Role.ROLE_USER);
    }

    @Test
    @DisplayName("Should register new user, create bank account, and return token")
    void testRegisterSuccess() {
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("newuser@dotlabs.ai")
                .password("Secret123!")
                .fullName("New User")
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@dotlabs.ai")).thenReturn(false);
        when(passwordEncoder.encode("Secret123!")).thenReturn("encoded_secret");
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtTokenProvider.generateTokenForUser(any(User.class))).thenReturn("new.jwt.token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("newuser");
        assertThat(response.getAccessToken()).isEqualTo("new.jwt.token");
        assertThat(response.getAccountNumber()).startsWith("100");

        verify(accountRepository).save(any(Account.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when username already exists during registration")
    void testRegisterDuplicateUsername() {
        RegisterRequest request = RegisterRequest.builder()
                .username("emmanuel")
                .email("other@dotlabs.ai")
                .password("Secret123!")
                .fullName("Emmanuel Duplicate")
                .build();

        when(userRepository.existsByUsername("emmanuel")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(InvalidTransferException.class)
                .hasMessageContaining("Username is already taken");
    }
}
