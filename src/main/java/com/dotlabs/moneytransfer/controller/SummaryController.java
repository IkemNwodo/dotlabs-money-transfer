package com.dotlabs.moneytransfer.controller;

import com.dotlabs.moneytransfer.dto.response.ApiResponse;
import com.dotlabs.moneytransfer.dto.response.DailySummaryResponse;
import com.dotlabs.moneytransfer.entity.DailyTransactionSummary;
import com.dotlabs.moneytransfer.service.SummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/v1/summaries")
@RequiredArgsConstructor
@Tag(name = "Daily Summaries", description = "Endpoints for generating and viewing daily transaction summaries")
public class SummaryController {

    private final SummaryService summaryService;

    @GetMapping("/daily")
    @Operation(
            summary = "Produce summary of transactions for a specified day",
            description = "Calculates total transactions, successful volume, transaction fees, and commissions for a given day (defaults to today)."
    )
    public ResponseEntity<ApiResponse<DailySummaryResponse>> getDailySummary(
            @Parameter(description = "Date for the summary (YYYY-MM-DD), defaults to current date if omitted", example = "2026-08-28")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        log.info("Fetching daily summary for date: {}", targetDate);
        DailySummaryResponse response = summaryService.getDailySummary(targetDate);
        return ResponseEntity.ok(ApiResponse.success(response, "Daily summary generated successfully"));
    }

    @PostMapping("/daily/persist")
    @Operation(
            summary = "Generate and persist daily transaction summary",
            description = "Computes metrics and persists the summary record into the database for archiving."
    )
    public ResponseEntity<ApiResponse<DailyTransactionSummary>> persistDailySummary(
            @Parameter(description = "Date to persist (defaults to yesterday if omitted)", example = "2026-08-27")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now().minusDays(1);
        log.info("Persisting daily summary for date: {}", targetDate);
        DailyTransactionSummary summary = summaryService.generateAndPersistDailySummary(targetDate);
        return ResponseEntity.ok(ApiResponse.success(summary, "Daily summary persisted successfully"));
    }
}
