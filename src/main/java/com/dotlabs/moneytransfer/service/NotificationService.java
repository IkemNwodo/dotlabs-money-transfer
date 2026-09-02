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
        log.info("Dispatching transfer OTP for user {} (account {}): OTP={}, expires in {}s",
                username, accountNumber, otpCode, expirationSeconds);
    }
}
