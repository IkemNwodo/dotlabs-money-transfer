package com.dotlabs.moneytransfer.dto.response;

import com.dotlabs.moneytransfer.enums.Currency;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Account details")
public class AccountResponse {

    @Schema(description = "Account number", example = "1000000001")
    private String accountNumber;

    @Schema(description = "Name of the account holder", example = "Ada Lovelace")
    private String accountHolderName;

    @Schema(description = "Current available balance", example = "100000.00")
    private BigDecimal balance;

    @Schema(description = "Account currency", example = "NGN")
    private Currency currency;
}
