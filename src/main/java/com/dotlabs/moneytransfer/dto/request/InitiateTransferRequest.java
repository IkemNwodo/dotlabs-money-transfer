package com.dotlabs.moneytransfer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload to initiate a 2FA-secured money transfer")
public class InitiateTransferRequest {

    @NotBlank(message = "Source account number is required")
    @Size(min = 5, max = 32, message = "Source account number must be between 5 and 32 characters")
    @Schema(description = "Account number of the sender (must be owned by authenticated user)", example = "1000000001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sourceAccountNumber;

    @NotBlank(message = "Destination account number is required")
    @Size(min = 5, max = 32, message = "Destination account number must be between 5 and 32 characters")
    @Schema(description = "Account number of the recipient", example = "1000000002", requiredMode = Schema.RequiredMode.REQUIRED)
    private String destinationAccountNumber;

    @NotNull(message = "Transfer amount is required")
    @DecimalMin(value = "0.01", message = "Transfer amount must be at least 0.01")
    @Schema(description = "Transfer principal amount", example = "5000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    @Schema(description = "Transfer description", example = "Payment for design services")
    private String description;

    @Size(max = 64, message = "Transaction reference must not exceed 64 characters")
    @Schema(description = "Optional transaction reference for idempotency", example = "TX-20260901-001")
    private String transactionReference;
}
