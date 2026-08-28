package com.dotlabs.moneytransfer.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FeeCalculatorTest {

    @Test
    @DisplayName("Should calculate 0.5% fee accurately for standard amounts")
    void testStandardFeeCalculation() {
        BigDecimal amount = new BigDecimal("1000.00");
        BigDecimal expectedFee = new BigDecimal("5.00"); // 1000 * 0.005 = 5.00

        BigDecimal fee = FeeCalculator.calculateTransactionFee(amount);
        assertThat(fee).isEqualByComparingTo(expectedFee);
    }

    @Test
    @DisplayName("Should cap transaction fee at 100.00 when 0.5% exceeds cap")
    void testFeeCapAt100() {
        // 20,000 * 0.005 = 100.00 (exact threshold)
        BigDecimal exactCapAmount = new BigDecimal("20000.00");
        assertThat(FeeCalculator.calculateTransactionFee(exactCapAmount)).isEqualByComparingTo("100.00");

        // 50,000 * 0.005 = 250.00 -> should cap at 100.00
        BigDecimal largeAmount = new BigDecimal("50000.00");
        assertThat(FeeCalculator.calculateTransactionFee(largeAmount)).isEqualByComparingTo("100.00");

        // 1,000,000 * 0.005 = 5000.00 -> should cap at 100.00
        BigDecimal millionAmount = new BigDecimal("1000000.00");
        assertThat(FeeCalculator.calculateTransactionFee(millionAmount)).isEqualByComparingTo("100.00");
    }

    @ParameterizedTest
    @CsvSource({
            "100.00, 0.50, 100.50",
            "500.00, 2.50, 502.50",
            "1000.00, 5.00, 1005.00",
            "20000.00, 100.00, 20100.00",
            "100000.00, 100.00, 100100.00"
    })
    @DisplayName("Should calculate billed amount as amount + fee")
    void testBilledAmountCalculation(String amountStr, String feeStr, String expectedBilledStr) {
        BigDecimal amount = new BigDecimal(amountStr);
        BigDecimal fee = new BigDecimal(feeStr);
        BigDecimal billed = FeeCalculator.calculateBilledAmount(amount, fee);

        assertThat(billed).isEqualByComparingTo(expectedBilledStr);
    }

    @Test
    @DisplayName("Should calculate commission as exactly 20% of transaction fee")
    void testCommissionCalculation() {
        // Fee of 5.00 -> 20% commission = 1.00
        BigDecimal fee = new BigDecimal("5.00");
        BigDecimal commission = FeeCalculator.calculateCommission(fee);
        assertThat(commission).isEqualByComparingTo("1.00");

        // Max fee 100.00 -> 20% commission = 20.00
        BigDecimal maxFee = new BigDecimal("100.00");
        BigDecimal maxCommission = FeeCalculator.calculateCommission(maxFee);
        assertThat(maxCommission).isEqualByComparingTo("20.00");

        // Fee of 0 -> 0 commission
        assertThat(FeeCalculator.calculateCommission(BigDecimal.ZERO)).isEqualByComparingTo("0.00");
    }
}
