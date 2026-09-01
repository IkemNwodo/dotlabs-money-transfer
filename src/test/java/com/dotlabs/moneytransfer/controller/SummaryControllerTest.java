package com.dotlabs.moneytransfer.controller;

import com.dotlabs.moneytransfer.dto.response.DailySummaryResponse;
import com.dotlabs.moneytransfer.security.CustomUserDetailsService;
import com.dotlabs.moneytransfer.security.JwtTokenProvider;
import com.dotlabs.moneytransfer.service.SummaryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SummaryController.class)
@AutoConfigureMockMvc(addFilters = false)
class SummaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SummaryService summaryService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("GET /api/v1/summaries/daily - Should return summary metrics")
    void testGetDailySummary() throws Exception {
        LocalDate date = LocalDate.of(2026, 8, 28);
        DailySummaryResponse summary = DailySummaryResponse.builder()
                .summaryDate(date)
                .totalTransactions(10)
                .successfulTransactions(8)
                .insufficientFundsTransactions(2)
                .failedTransactions(0)
                .totalSuccessfulVolume(new BigDecimal("80000.00"))
                .totalFees(new BigDecimal("400.00"))
                .totalCommission(new BigDecimal("80.00"))
                .statusBreakdown(Map.of("SUCCESSFUL", 8L, "INSUFFICIENT_FUNDS", 2L))
                .build();

        when(summaryService.getDailySummary(any(LocalDate.class))).thenReturn(summary);

        mockMvc.perform(get("/api/v1/summaries/daily")
                        .param("date", "2026-08-28")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalTransactions").value(10))
                .andExpect(jsonPath("$.data.successfulTransactions").value(8))
                .andExpect(jsonPath("$.data.totalSuccessfulVolume").value(80000.00))
                .andExpect(jsonPath("$.data.totalFees").value(400.00))
                .andExpect(jsonPath("$.data.totalCommission").value(80.00));
    }
}
