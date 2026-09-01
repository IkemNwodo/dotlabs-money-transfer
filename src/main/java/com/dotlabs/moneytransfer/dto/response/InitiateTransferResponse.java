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
@Schema(description = "Response returned upon successful initiation of a 2FA transfer")
public class InitiateTransferResponse {

    @Schema(description = "Unique transfer session ID to supply during authorization", example = "OTP-SES-8F92D1B3")
    private String sessionId;

    @Schema(description = "Source account number", example = "1000000001")
    private String sourceAccountNumber;

    @Schema(description = "Destination account number", example = "1000000002")
    private String destinationAccountNumber;

    @Schema(description = "Transfer principal amount", example = "5000.00")
    private BigDecimal amount;

    @Schema(description = "Calculated transaction fee", example = "25.00")
    private BigDecimal transactionFee;

    @Schema(description = "Total amount to be debited", example = "5025.00")
    private BigDecimal billedAmount;

    @Schema(description = "Session and OTP expiration timestamp")
    private LocalDateTime expiresAt;

    @Schema(description = "Instruction message", example = "OTP has been sent to your registered contact channel. Please authorize within 5 minutes.")
    private String message;
}
