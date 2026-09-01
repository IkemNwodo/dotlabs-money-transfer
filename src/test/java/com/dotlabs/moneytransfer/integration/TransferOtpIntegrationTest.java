package com.dotlabs.moneytransfer.integration;

import com.dotlabs.moneytransfer.dto.request.AuthorizeTransferRequest;
import com.dotlabs.moneytransfer.dto.request.InitiateTransferRequest;
import com.dotlabs.moneytransfer.dto.request.LoginRequest;
import com.dotlabs.moneytransfer.dto.response.AuthResponse;
import com.dotlabs.moneytransfer.dto.response.InitiateTransferResponse;
import com.dotlabs.moneytransfer.dto.response.TransferResponse;
import com.dotlabs.moneytransfer.entity.Account;
import com.dotlabs.moneytransfer.entity.TransferOtpSession;
import com.dotlabs.moneytransfer.entity.User;
import com.dotlabs.moneytransfer.enums.Currency;
import com.dotlabs.moneytransfer.enums.Role;
import com.dotlabs.moneytransfer.enums.TransactionStatus;
import com.dotlabs.moneytransfer.repository.AccountRepository;
import com.dotlabs.moneytransfer.repository.TransferOtpSessionRepository;
import com.dotlabs.moneytransfer.repository.UserRepository;
import com.dotlabs.moneytransfer.service.AuthService;
import com.dotlabs.moneytransfer.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class TransferOtpIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransferOtpSessionRepository otpSessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String SENDER_ACC = "2000000001";
    private static final String RECIPIENT_ACC = "2000000002";
    private static final String TEST_USER = "test_emmanuel";
    private static final String TEST_PASS = "Password123!";

    @BeforeEach
    void setUp() {
        if (!accountRepository.existsByAccountNumber(SENDER_ACC)) {
            accountRepository.save(Account.builder()
                    .accountNumber(SENDER_ACC)
                    .accountHolderName("Test Sender Emmanuel")
                    .balance(new BigDecimal("100000.00"))
                    .currency(Currency.NGN)
                    .build());
        }

        if (!accountRepository.existsByAccountNumber(RECIPIENT_ACC)) {
            accountRepository.save(Account.builder()
                    .accountNumber(RECIPIENT_ACC)
                    .accountHolderName("Test Recipient Ekene")
                    .balance(new BigDecimal("50000.00"))
                    .currency(Currency.NGN)
                    .build());
        }

        if (!userRepository.existsByUsername(TEST_USER)) {
            userRepository.save(User.builder()
                    .username(TEST_USER)
                    .email("test.emmanuel@dotlabs.ai")
                    .password(passwordEncoder.encode(TEST_PASS))
                    .role(Role.ROLE_USER)
                    .accountNumber(SENDER_ACC)
                    .build());
        }
    }

    @Test
    @DisplayName("End-to-End: Authenticate, Initiate 2FA Transfer, Authorize with OTP, and Mutate Balances")
    void testEndToEnd2faTransferWorkflow() {
        // 1. Authenticate user Emmanuel
        LoginRequest loginRequest = LoginRequest.builder()
                .username(TEST_USER)
                .password(TEST_PASS)
                .build();
        AuthResponse authResponse = authService.login(loginRequest);
        assertThat(authResponse.getAccessToken()).isNotBlank();

        User authenticatedUser = userRepository.findByUsername(TEST_USER).orElseThrow();
        Account initialSender = accountRepository.findByAccountNumber(SENDER_ACC).orElseThrow();
        Account initialRecipient = accountRepository.findByAccountNumber(RECIPIENT_ACC).orElseThrow();

        BigDecimal transferAmount = new BigDecimal("5000.00");
        BigDecimal expectedFee = new BigDecimal("25.00"); // 5000 * 0.005 = 25.00
        BigDecimal expectedBilled = new BigDecimal("5025.00");

        // 2. Step 1: Initiate Transfer
        InitiateTransferRequest initiateRequest = InitiateTransferRequest.builder()
                .sourceAccountNumber(SENDER_ACC)
                .destinationAccountNumber(RECIPIENT_ACC)
                .amount(transferAmount)
                .description("Consulting fee")
                .build();

        InitiateTransferResponse initResponse = transferService.initiateTransfer(initiateRequest, authenticatedUser);
        assertThat(initResponse.getSessionId()).isNotBlank();
        assertThat(initResponse.getBilledAmount()).isEqualByComparingTo(expectedBilled);

        // 3. Retrieve generated OTP from database session
        TransferOtpSession session = otpSessionRepository.findBySessionId(initResponse.getSessionId()).orElseThrow();
        String generatedOtp = session.getOtpCode();
        assertThat(generatedOtp).matches("^\\d{6}$");

        // 4. Step 2: Authorize Transfer with the OTP
        AuthorizeTransferRequest authTransferRequest = AuthorizeTransferRequest.builder()
                .sessionId(initResponse.getSessionId())
                .otpCode(generatedOtp)
                .build();

        TransferResponse transferResult = transferService.authorizeTransfer(authTransferRequest, authenticatedUser);

        // 5. Verify Transaction outcome
        assertThat(transferResult.getStatus()).isEqualTo(TransactionStatus.SUCCESSFUL);
        assertThat(transferResult.getAmount()).isEqualByComparingTo(transferAmount);
        assertThat(transferResult.getTransactionFee()).isEqualByComparingTo(expectedFee);
        assertThat(transferResult.getBilledAmount()).isEqualByComparingTo(expectedBilled);

        // 6. Verify final account balances
        Account finalSender = accountRepository.findByAccountNumber(SENDER_ACC).orElseThrow();
        Account finalRecipient = accountRepository.findByAccountNumber(RECIPIENT_ACC).orElseThrow();

        assertThat(finalSender.getBalance()).isEqualByComparingTo(initialSender.getBalance().subtract(expectedBilled));
        assertThat(finalRecipient.getBalance()).isEqualByComparingTo(initialRecipient.getBalance().add(transferAmount));
    }
}
