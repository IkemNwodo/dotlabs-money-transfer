package com.dotlabs.moneytransfer.service;

import com.dotlabs.moneytransfer.dto.request.InitiateTransferRequest;
import com.dotlabs.moneytransfer.entity.TransferOtpSession;
import com.dotlabs.moneytransfer.entity.User;
import com.dotlabs.moneytransfer.enums.OtpSessionStatus;
import com.dotlabs.moneytransfer.enums.Role;
import com.dotlabs.moneytransfer.exception.InvalidTransferException;
import com.dotlabs.moneytransfer.repository.TransferOtpSessionRepository;
import com.dotlabs.moneytransfer.service.impl.OtpServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private TransferOtpSessionRepository otpSessionRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OtpServiceImpl otpService;

    private User testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(otpService, "otpExpirationSeconds", 300);
        ReflectionTestUtils.setField(otpService, "maxAttempts", 3);

        testUser = User.builder()
                .id(1L)
                .username("emmanuel")
                .email("emmanuel@dotlabs.ai")
                .role(Role.ROLE_USER)
                .accountNumber("1000000001")
                .build();
    }

    @Test
    @DisplayName("Should create OTP session with 6-digit code and dispatch notification")
    void testCreateTransferOtpSession() {
        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .sourceAccountNumber("1000000001")
                .destinationAccountNumber("1000000002")
                .amount(new BigDecimal("5000.00"))
                .description("Test transfer")
                .build();

        when(otpSessionRepository.save(any(TransferOtpSession.class))).thenAnswer(inv -> inv.getArgument(0));

        TransferOtpSession session = otpService.createTransferOtpSession(
                testUser,
                request,
                new BigDecimal("25.00"),
                new BigDecimal("5025.00")
        );

        assertThat(session).isNotNull();
        assertThat(session.getSessionId()).startsWith("OTP-SES-");
        assertThat(session.getOtpCode()).matches("^\\d{6}$");
        assertThat(session.getStatus()).isEqualTo(OtpSessionStatus.PENDING);
        assertThat(session.getExpiresAt()).isAfter(LocalDateTime.now());

        verify(notificationService).sendTransferOtp(eq("emmanuel"), eq("1000000001"), eq(session.getOtpCode()), eq(300));
        verify(otpSessionRepository).save(any(TransferOtpSession.class));
    }

    @Test
    @DisplayName("Should successfully validate correct OTP code")
    void testValidateCorrectOtp() {
        TransferOtpSession session = TransferOtpSession.builder()
                .sessionId("OTP-SES-12345")
                .username("emmanuel")
                .otpCode("492018")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .status(OtpSessionStatus.PENDING)
                .attempts(0)
                .build();

        when(otpSessionRepository.findBySessionId("OTP-SES-12345")).thenReturn(Optional.of(session));
        when(otpSessionRepository.save(any(TransferOtpSession.class))).thenAnswer(inv -> inv.getArgument(0));

        TransferOtpSession verified = otpService.validateAndConsumeOtp("OTP-SES-12345", "492018", "emmanuel");

        assertThat(verified.getStatus()).isEqualTo(OtpSessionStatus.COMPLETED);
    }

    @Test
    @DisplayName("Should increment attempts on wrong OTP and lock session after 3 failures")
    void testWrongOtpIncrementsAttemptsAndLocks() {
        TransferOtpSession session = TransferOtpSession.builder()
                .sessionId("OTP-SES-12345")
                .username("emmanuel")
                .otpCode("492018")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .status(OtpSessionStatus.PENDING)
                .attempts(2) // 2 attempts already used
                .build();

        when(otpSessionRepository.findBySessionId("OTP-SES-12345")).thenReturn(Optional.of(session));
        when(otpSessionRepository.save(any(TransferOtpSession.class))).thenAnswer(inv -> inv.getArgument(0));

        // 3rd failed attempt
        assertThatThrownBy(() -> otpService.validateAndConsumeOtp("OTP-SES-12345", "000000", "emmanuel"))
                .isInstanceOf(InvalidTransferException.class)
                .hasMessageContaining("Maximum verification attempts exceeded");

        assertThat(session.getStatus()).isEqualTo(OtpSessionStatus.FAILED);
        assertThat(session.getAttempts()).isEqualTo(3);
    }
}
