package com.dotlabs.moneytransfer.dto.response;

import com.dotlabs.moneytransfer.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Authentication response payload containing JWT access token")
public class AuthResponse {

    @Schema(description = "JWT Access Token for Bearer authentication", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Builder.Default
    @Schema(description = "Token type", example = "Bearer")
    private String tokenType = "Bearer";

    @Schema(description = "Authenticated username", example = "emmanuel")
    private String username;

    @Schema(description = "User role", example = "ROLE_USER")
    private Role role;

    @Schema(description = "User's assigned bank account number", example = "1000000001")
    private String accountNumber;

    @Schema(description = "Token expiration duration in milliseconds", example = "86400000")
    private long expiresInMs;
}
