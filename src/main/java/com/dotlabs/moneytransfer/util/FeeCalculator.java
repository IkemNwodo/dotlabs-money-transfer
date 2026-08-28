package com.dotlabs.moneytransfer.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility class for calculating transaction fees and commissions.
 *
 * Rules:
 * - Transaction fee is 0.5% (0.005) of original amount, capped at 100.00.
 * - Commission on each successful transaction is 20% (0.20) of the transaction fee.
 * - Billed amount is amount + transaction fee.
 */
public final class FeeCalculator {

    public static final BigDecimal FEE_PERCENTAGE = new BigDecimal("0.005"); // 0.5%
    public static final BigDecimal FEE_CAP = new BigDecimal("100.00");
    public static final BigDecimal COMMISSION_PERCENTAGE = new BigDecimal("0.20"); // 20%

    private FeeCalculator() {
        // Utility class
    }

    /**
     * Calculates the transaction fee for a given transfer amount.
     * Fee = min(amount * 0.005, 100.00)
     */
    public static BigDecimal calculateTransactionFee(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal calculatedFee = amount.multiply(FEE_PERCENTAGE).setScale(2, RoundingMode.HALF_UP);
        return calculatedFee.compareTo(FEE_CAP) > 0 ? FEE_CAP : calculatedFee;
    }

    /**
     * Calculates the total billed amount (amount + fee).
     */
    public static BigDecimal calculateBilledAmount(BigDecimal amount, BigDecimal fee) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal safeFee = (fee != null) ? fee : calculateTransactionFee(amount);
        return amount.add(safeFee).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the commission from a transaction fee.
     * Commission = 20% of transaction fee.
     */
    public static BigDecimal calculateCommission(BigDecimal transactionFee) {
        if (transactionFee == null || transactionFee.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return transactionFee.multiply(COMMISSION_PERCENTAGE).setScale(2, RoundingMode.HALF_UP);
    }
}
