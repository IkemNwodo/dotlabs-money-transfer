package com.dotlabs.moneytransfer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload to authorize and execute a transfer with an OTP")
public class AuthorizeTransferRequest {

    @NotBlank(message = "Session ID is required")
    @Schema(description = "OTP Transfer Session ID returned during initiation", example = "OTP-SES-8F92D1B3", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sessionId;

    @NotBlank(message = "OTP code is required")
    @Pattern(regexp = "^\\d{6}$", message = "OTP must be a 6-digit numeric code")
    @Schema(description = "6-digit OTP code received by the user", example = "492018", requiredMode = Schema.RequiredMode.REQUIRED)
    private String otpCode;
}
