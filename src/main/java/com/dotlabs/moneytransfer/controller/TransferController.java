package com.dotlabs.moneytransfer.controller;

import com.dotlabs.moneytransfer.dto.request.TransferRequest;
import com.dotlabs.moneytransfer.dto.response.ApiResponse;
import com.dotlabs.moneytransfer.dto.response.ErrorResponse;
import com.dotlabs.moneytransfer.dto.response.TransferResponse;
import com.dotlabs.moneytransfer.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
@Tag(name = "Money Transfers", description = "Endpoints for initiating and processing money transfers between bank accounts")
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    @Operation(
            summary = "Accept and process a money transfer",
            description = "Simulates money transfer between two accounts with fee calculation (0.5% capped at 100) and balance updates."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Transfer processed successfully (or recorded with INSUFFICIENT FUND status)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid transfer request or duplicate reference",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Sender or recipient account not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<TransferResponse>> transferMoney(@Valid @RequestBody TransferRequest request) {
        log.info("Received transfer request from {} to {} of amount {}",
                request.getSourceAccountNumber(), request.getDestinationAccountNumber(), request.getAmount());
        
        TransferResponse response = transferService.processTransfer(request);
        return ResponseEntity.ok(ApiResponse.success(response, response.getStatusMessage()));
    }
}
