package com.dotlabs.moneytransfer.controller;

import com.dotlabs.moneytransfer.dto.request.TransactionFilterRequest;
import com.dotlabs.moneytransfer.dto.response.TransactionResponse;
import com.dotlabs.moneytransfer.enums.TransactionStatus;
import com.dotlabs.moneytransfer.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @Test
    @DisplayName("GET /api/v1/transactions - Should return paginated list of transactions")
    void testGetTransactionsList() throws Exception {
        TransactionResponse tx = TransactionResponse.builder()
                .id(1L)
                .transactionReference("TX-TEST-001")
                .sourceAccountNumber("1000000001")
                .destinationAccountNumber("1000000002")
                .amount(new BigDecimal("1000.00"))
                .transactionFee(new BigDecimal("5.00"))
                .billedAmount(new BigDecimal("1005.00"))
                .status(TransactionStatus.SUCCESSFUL)
                .statusMessage("Transfer completed successfully")
                .dateCreated(LocalDateTime.now())
                .build();

        when(transactionService.getTransactions(any(TransactionFilterRequest.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(tx)));

        mockMvc.perform(get("/api/v1/transactions")
                        .param("status", "SUCCESSFUL")
                        .param("accountNumber", "1000000001")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].transactionReference").value("TX-TEST-001"))
                .andExpect(jsonPath("$.data.content[0].amount").value(1000.00));
    }

    @Test
    @DisplayName("GET /api/v1/transactions/{ref} - Should return single transaction")
    void testGetTransactionByReference() throws Exception {
        TransactionResponse tx = TransactionResponse.builder()
                .id(1L)
                .transactionReference("TX-TEST-001")
                .sourceAccountNumber("1000000001")
                .destinationAccountNumber("1000000002")
                .amount(new BigDecimal("1000.00"))
                .status(TransactionStatus.SUCCESSFUL)
                .build();

        when(transactionService.getTransactionByReference("TX-TEST-001")).thenReturn(tx);

        mockMvc.perform(get("/api/v1/transactions/TX-TEST-001")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.transactionReference").value("TX-TEST-001"));
    }
}
