package com.dotlabs.moneytransfer.security;

import com.dotlabs.moneytransfer.entity.User;
import com.dotlabs.moneytransfer.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", secret);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", 3600000L); // 1 hour
    }

    @Test
    @DisplayName("Should generate and validate JWT token successfully")
    void testGenerateAndValidateToken() {
        User user = User.builder()
                .username("testuser")
                .email("test@dotlabs.ai")
                .role(Role.ROLE_USER)
                .accountNumber("1000000001")
                .build();

        String token = jwtTokenProvider.generateTokenForUser(user);

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUsernameFromToken(token)).isEqualTo("testuser");
        assertThat(jwtTokenProvider.getAccountNumberFromToken(token)).isEqualTo("1000000001");
    }

    @Test
    @DisplayName("Should reject invalid or tampered JWT token")
    void testInvalidToken() {
        assertThat(jwtTokenProvider.validateToken("invalid.jwt.token")).isFalse();
    }
}
