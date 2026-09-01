package com.dotlabs.moneytransfer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Login credentials request payload")
public class LoginRequest {

    @NotBlank(message = "Username is required")
    @Schema(description = "User's unique username", example = "emmanuel", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "Password is required")
    @Schema(description = "User password", example = "Password123!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}
