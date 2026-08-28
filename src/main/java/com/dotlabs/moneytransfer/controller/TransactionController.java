package com.dotlabs.moneytransfer.controller;

import com.dotlabs.moneytransfer.dto.request.TransactionFilterRequest;
import com.dotlabs.moneytransfer.dto.response.ApiResponse;
import com.dotlabs.moneytransfer.dto.response.TransactionResponse;
import com.dotlabs.moneytransfer.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Endpoints for querying and retrieving transaction records")
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    @Operation(
            summary = "Retrieve a list of transactions with optional filters",
            description = "Filter transactions by status, account number (sender/recipient), and date range with pagination support."
    )
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getTransactions(
            @ParameterObject TransactionFilterRequest filter,
            @ParameterObject @PageableDefault(size = 20, sort = "dateCreated", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.info("Querying transactions with filter: {} and pageable: {}", filter, pageable);
        Page<TransactionResponse> result = transactionService.getTransactions(filter, pageable);
        return ResponseEntity.ok(ApiResponse.success(result, "Transactions retrieved successfully"));
    }

    @GetMapping("/{reference}")
    @Operation(
            summary = "Retrieve a transaction by its reference",
            description = "Fetches complete details of a single transaction using its unique transaction reference."
    )
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransactionByReference(
            @Parameter(description = "Unique transaction reference", example = "TX-8FA7D21C-9E41")
            @PathVariable String reference
    ) {
        log.info("Fetching transaction by reference: {}", reference);
        TransactionResponse result = transactionService.getTransactionByReference(reference);
        return ResponseEntity.ok(ApiResponse.success(result, "Transaction found"));
    }
}
