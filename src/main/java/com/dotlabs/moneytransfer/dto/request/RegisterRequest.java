package com.dotlabs.moneytransfer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User registration payload")
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 32, message = "Username must be between 3 and 32 characters")
    @Schema(description = "Desired unique username", example = "newuser", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    @Schema(description = "User email address", example = "user@dotlabs.ai", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 64, message = "Password must be between 6 and 64 characters")
    @Schema(description = "Account password", example = "SecurePass123!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotBlank(message = "Account holder full name is required")
    @Schema(description = "Full name of the account holder for bank account creation", example = "John Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fullName;
}
