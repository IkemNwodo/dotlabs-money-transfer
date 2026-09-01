package com.dotlabs.moneytransfer.controller;

import com.dotlabs.moneytransfer.dto.request.AuthorizeTransferRequest;
import com.dotlabs.moneytransfer.dto.request.InitiateTransferRequest;
import com.dotlabs.moneytransfer.dto.request.TransferRequest;
import com.dotlabs.moneytransfer.dto.response.ApiResponse;
import com.dotlabs.moneytransfer.dto.response.ErrorResponse;
import com.dotlabs.moneytransfer.dto.response.InitiateTransferResponse;
import com.dotlabs.moneytransfer.dto.response.TransferResponse;
import com.dotlabs.moneytransfer.entity.User;
import com.dotlabs.moneytransfer.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
@Tag(name = "Money Transfers", description = "Endpoints for direct and 2FA OTP-secured money transfers between bank accounts")
@SecurityRequirement(name = "BearerAuth")
public class TransferController {

    private final TransferService transferService;

    @PostMapping("/initiate")
    @Operation(
            summary = "Initiate a 2FA-secured money transfer",
            description = "Step 1 of 2FA transfer: validates funds and generates a 6-digit OTP dispatched to user's registered contact channel."
    )
    public ResponseEntity<ApiResponse<InitiateTransferResponse>> initiateTransfer(
            @Valid @RequestBody InitiateTransferRequest request,
            @AuthenticationPrincipal User user
    ) {
        log.info("Received transfer initiation from {} for amount {}", request.getSourceAccountNumber(), request.getAmount());
        InitiateTransferResponse response = transferService.initiateTransfer(request, user);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    @PostMapping("/authorize")
    @Operation(
            summary = "Authorize and execute a transfer with OTP",
            description = "Step 2 of 2FA transfer: verifies the 6-digit OTP code against the session ID and executes atomic transfer."
    )
    public ResponseEntity<ApiResponse<TransferResponse>> authorizeTransfer(
            @Valid @RequestBody AuthorizeTransferRequest request,
            @AuthenticationPrincipal User user
    ) {
        log.info("Received transfer authorization for session {}", request.getSessionId());
        TransferResponse response = transferService.authorizeTransfer(request, user);
        return ResponseEntity.ok(ApiResponse.success(response, response.getStatusMessage()));
    }

    @PostMapping
    @Operation(
            summary = "Direct transfer execution",
            description = "Simulates direct money transfer between two accounts with fee calculation (0.5% capped at 100) and balance updates."
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
        log.info("Received direct transfer request from {} to {} of amount {}",
                request.getSourceAccountNumber(), request.getDestinationAccountNumber(), request.getAmount());

        TransferResponse response = transferService.processTransfer(request);
        return ResponseEntity.ok(ApiResponse.success(response, response.getStatusMessage()));
    }
}
