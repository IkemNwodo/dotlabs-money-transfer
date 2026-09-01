package com.dotlabs.moneytransfer.controller;

import com.dotlabs.moneytransfer.dto.request.AuthorizeTransferRequest;
import com.dotlabs.moneytransfer.dto.request.InitiateTransferRequest;
import com.dotlabs.moneytransfer.dto.request.TransferRequest;
import com.dotlabs.moneytransfer.dto.response.InitiateTransferResponse;
import com.dotlabs.moneytransfer.dto.response.TransferResponse;
import com.dotlabs.moneytransfer.enums.TransactionStatus;
import com.dotlabs.moneytransfer.exception.AccountNotFoundException;
import com.dotlabs.moneytransfer.security.CustomUserDetailsService;
import com.dotlabs.moneytransfer.security.JwtTokenProvider;
import com.dotlabs.moneytransfer.service.TransferService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransferController.class)
@AutoConfigureMockMvc(addFilters = false)
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransferService transferService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("POST /api/v1/transfers - Should return 200 and success response when valid")
    void testProcessTransferSuccess() throws Exception {
        TransferRequest request = TransferRequest.builder()
                .sourceAccountNumber("1000000001")
                .destinationAccountNumber("1000000002")
                .amount(new BigDecimal("5000.00"))
                .description("Test transfer")
                .build();

        TransferResponse response = TransferResponse.builder()
                .transactionReference("TX-ABC123XYZ")
                .sourceAccountNumber("1000000001")
                .destinationAccountNumber("1000000002")
                .amount(new BigDecimal("5000.00"))
                .transactionFee(new BigDecimal("25.00"))
                .billedAmount(new BigDecimal("5025.00"))
                .status(TransactionStatus.SUCCESSFUL)
                .statusMessage("Transfer completed successfully")
                .dateCreated(LocalDateTime.now())
                .build();

        when(transferService.processTransfer(any(TransferRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.transactionReference").value("TX-ABC123XYZ"))
                .andExpect(jsonPath("$.data.amount").value(5000.00))
                .andExpect(jsonPath("$.data.transactionFee").value(25.00))
                .andExpect(jsonPath("$.data.billedAmount").value(5025.00))
                .andExpect(jsonPath("$.data.status").value("SUCCESSFUL"));
    }

    @Test
    @DisplayName("POST /api/v1/transfers/initiate - Should initiate 2FA transfer")
    void testInitiateTransferSuccess() throws Exception {
        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .sourceAccountNumber("1000000001")
                .destinationAccountNumber("1000000002")
                .amount(new BigDecimal("5000.00"))
                .build();

        InitiateTransferResponse response = InitiateTransferResponse.builder()
                .sessionId("OTP-SES-12345")
                .sourceAccountNumber("1000000001")
                .destinationAccountNumber("1000000002")
                .amount(new BigDecimal("5000.00"))
                .transactionFee(new BigDecimal("25.00"))
                .billedAmount(new BigDecimal("5025.00"))
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .message("OTP sent")
                .build();

        when(transferService.initiateTransfer(any(InitiateTransferRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/transfers/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sessionId").value("OTP-SES-12345"));
    }

    @Test
    @DisplayName("POST /api/v1/transfers/authorize - Should authorize transfer with OTP")
    void testAuthorizeTransferSuccess() throws Exception {
        AuthorizeTransferRequest request = AuthorizeTransferRequest.builder()
                .sessionId("OTP-SES-12345")
                .otpCode("492018")
                .build();

        TransferResponse response = TransferResponse.builder()
                .transactionReference("TX-ABC123XYZ")
                .sourceAccountNumber("1000000001")
                .destinationAccountNumber("1000000002")
                .amount(new BigDecimal("5000.00"))
                .status(TransactionStatus.SUCCESSFUL)
                .statusMessage("Transfer completed successfully")
                .build();

        when(transferService.authorizeTransfer(any(AuthorizeTransferRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/transfers/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SUCCESSFUL"));
    }

    @Test
    @DisplayName("POST /api/v1/transfers - Should return 400 when request body fails validation")
    void testProcessTransferValidationFailure() throws Exception {
        TransferRequest invalidRequest = TransferRequest.builder()
                .sourceAccountNumber("") // blank
                .destinationAccountNumber("1000000002")
                .amount(new BigDecimal("-100.00")) // negative
                .build();

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    @Test
    @DisplayName("POST /api/v1/transfers - Should return 404 when account does not exist")
    void testProcessTransferAccountNotFound() throws Exception {
        TransferRequest request = TransferRequest.builder()
                .sourceAccountNumber("9999999999")
                .destinationAccountNumber("1000000002")
                .amount(new BigDecimal("100.00"))
                .build();

        when(transferService.processTransfer(any(TransferRequest.class)))
                .thenThrow(new AccountNotFoundException("Source account not found: 9999999999"));

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Account Not Found"))
                .andExpect(jsonPath("$.message").value("Source account not found: 9999999999"));
    }
}
