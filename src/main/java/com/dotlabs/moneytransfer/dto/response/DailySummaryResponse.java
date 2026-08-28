package com.dotlabs.moneytransfer.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Aggregated daily transaction summary metrics")
public class DailySummaryResponse {

    @Schema(description = "The date for which the summary was calculated", example = "2026-08-28")
    private LocalDate summaryDate;

    @Schema(description = "Total number of transactions attempted on this day", example = "150")
    private long totalTransactions;

    @Schema(description = "Count of successful transactions", example = "135")
    private long successfulTransactions;

    @Schema(description = "Count of failed transactions due to insufficient funds", example = "10")
    private long insufficientFundsTransactions;

    @Schema(description = "Count of other failed transactions", example = "5")
    private long failedTransactions;

    @Schema(description = "Total volume (principal amount) of successful transfers", example = "2500000.00")
    private BigDecimal totalSuccessfulVolume;

    @Schema(description = "Total transaction fees earned from successful transfers", example = "12500.00")
    private BigDecimal totalFees;

    @Schema(description = "Total commission calculated for the day", example = "2500.00")
    private BigDecimal totalCommission;

    @Schema(description = "Breakdown of transaction counts by status")
    private Map<String, Long> statusBreakdown;
}
