package com.dotlabs.moneytransfer.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Summary of the executed commission analysis operation")
public class CommissionAnalysisResponse {

    @Schema(description = "Total number of transactions analyzed", example = "42")
    private int totalAnalyzed;

    @Schema(description = "Number of transactions updated as commission worthy", example = "35")
    private int commissionWorthyUpdated;

    @Schema(description = "Total commission amount calculated in this run", example = "175.50")
    private BigDecimal totalCommissionCalculated;

    @Schema(description = "Timestamp when the analysis completed")
    private LocalDateTime executionTimestamp;

    @Schema(description = "Execution duration in milliseconds", example = "48")
    private long executionDurationMs;
}
