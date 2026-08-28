package com.dotlabs.moneytransfer.dto.response;

import com.dotlabs.moneytransfer.enums.TransactionStatus;
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
@Schema(description = "Response payload after processing a money transfer")
public class TransferResponse {

    @Schema(description = "Unique transaction reference", example = "TX-8FA7D21C-9E41")
    private String transactionReference;

    @Schema(description = "Source account number", example = "1000000001")
    private String sourceAccountNumber;

    @Schema(description = "Destination account number", example = "1000000002")
    private String destinationAccountNumber;

    @Schema(description = "Transfer principal amount", example = "5000.00")
    private BigDecimal amount;

    @Schema(description = "Calculated transaction fee (0.5% capped at 100)", example = "25.00")
    private BigDecimal transactionFee;

    @Schema(description = "Total amount billed from source account (amount + fee)", example = "5025.00")
    private BigDecimal billedAmount;

    @Schema(description = "Status of the transfer", example = "SUCCESSFUL")
    private TransactionStatus status;

    @Schema(description = "Human-readable status message", example = "Transfer completed successfully")
    private String statusMessage;

    @Schema(description = "Transfer description", example = "Payment for consulting services")
    private String description;

    @Schema(description = "Timestamp when the transaction was created")
    private LocalDateTime dateCreated;
}
