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
@Schema(description = "Detailed transaction record")
public class TransactionResponse {

    @Schema(description = "Internal transaction ID", example = "1")
    private Long id;

    @Schema(description = "Unique transaction reference", example = "TX-8FA7D21C-9E41")
    private String transactionReference;

    @Schema(description = "Source account number", example = "1000000001")
    private String sourceAccountNumber;

    @Schema(description = "Destination account number", example = "1000000002")
    private String destinationAccountNumber;

    @Schema(description = "Transfer amount", example = "5000.00")
    private BigDecimal amount;

    @Schema(description = "Transaction fee", example = "25.00")
    private BigDecimal transactionFee;

    @Schema(description = "Billed amount", example = "5025.00")
    private BigDecimal billedAmount;

    @Schema(description = "Transaction description", example = "Payment for consulting services")
    private String description;

    @Schema(description = "Transaction status", example = "SUCCESSFUL")
    private TransactionStatus status;

    @Schema(description = "Status message", example = "Transfer completed successfully")
    private String statusMessage;

    @Schema(description = "Whether the transaction has been evaluated as commission worthy", example = "true")
    private Boolean commissionWorthy;

    @Schema(description = "Commission amount (20% of fee)", example = "5.00")
    private BigDecimal commission;

    @Schema(description = "Timestamp when commission analysis was executed")
    private LocalDateTime commissionProcessedAt;

    @Schema(description = "Date and time when the transaction was created")
    private LocalDateTime dateCreated;
}
