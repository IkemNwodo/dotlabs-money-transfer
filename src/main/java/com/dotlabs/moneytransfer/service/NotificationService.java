package com.dotlabs.moneytransfer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationService {

    /**
     * Dispatches OTP to user's registered communication channel (simulated via structured logger).
     */
    public void sendTransferOtp(String username, String accountNumber, String otpCode, int expirationSeconds) {
        log.info("\n" +
                "========================================================================\n" +
                "  📲 [NOTIFICATION DISPATCH: TRANSFER 2FA OTP]\n" +
                "  To User:         {}\n" +
                "  Account:         {}\n" +
                "  One-Time Code:   >>> {} <<<\n" +
                "  Expires In:      {} seconds (5 minutes)\n" +
                "  Security Note:   Do NOT share this code with anyone.\n" +
                "========================================================================",
                username, accountNumber, otpCode, expirationSeconds);
    }
}
