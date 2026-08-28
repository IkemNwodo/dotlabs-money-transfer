package com.dotlabs.moneytransfer.controller;

import com.dotlabs.moneytransfer.dto.request.TransferRequest;
import com.dotlabs.moneytransfer.dto.response.TransferResponse;
import com.dotlabs.moneytransfer.enums.TransactionStatus;
import com.dotlabs.moneytransfer.exception.AccountNotFoundException;
import com.dotlabs.moneytransfer.exception.InvalidTransferException;
import com.dotlabs.moneytransfer.service.TransferService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransferService transferService;

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
