package com.dotlabs.moneytransfer.dto.request;

import com.dotlabs.moneytransfer.enums.TransactionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Filter parameters for retrieving transactions")
public class TransactionFilterRequest {

    @Schema(description = "Filter by transaction status (e.g. SUCCESSFUL, INSUFFICIENT_FUNDS, FAILED)", example = "SUCCESSFUL")
    private TransactionStatus status;

    @Schema(description = "Filter by account number (matches either source or destination account)", example = "1000000001")
    private String accountNumber;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "Filter by start date (YYYY-MM-DD)", example = "2026-08-01")
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "Filter by end date (YYYY-MM-DD)", example = "2026-08-31")
    private LocalDate endDate;
}
