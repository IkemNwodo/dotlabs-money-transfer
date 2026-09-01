package com.dotlabs.moneytransfer.service;

import com.dotlabs.moneytransfer.dto.request.InitiateTransferRequest;
import com.dotlabs.moneytransfer.entity.TransferOtpSession;
import com.dotlabs.moneytransfer.entity.User;

import java.math.BigDecimal;

public interface OtpService {
    TransferOtpSession createTransferOtpSession(User user, InitiateTransferRequest request, BigDecimal fee, BigDecimal billedAmount);
    TransferOtpSession validateAndConsumeOtp(String sessionId, String otpCode, String username);
}
