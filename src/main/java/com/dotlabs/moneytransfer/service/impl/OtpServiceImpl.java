package com.dotlabs.moneytransfer.service.impl;

import com.dotlabs.moneytransfer.dto.request.InitiateTransferRequest;
import com.dotlabs.moneytransfer.entity.TransferOtpSession;
import com.dotlabs.moneytransfer.entity.User;
import com.dotlabs.moneytransfer.enums.OtpSessionStatus;
import com.dotlabs.moneytransfer.exception.InvalidTransferException;
import com.dotlabs.moneytransfer.repository.TransferOtpSessionRepository;
import com.dotlabs.moneytransfer.service.NotificationService;
import com.dotlabs.moneytransfer.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private final TransferOtpSessionRepository otpSessionRepository;
    private final NotificationService notificationService;

    @Value("${app.otp.expiration-seconds:300}")
    private int otpExpirationSeconds;

    @Value("${app.otp.max-attempts:3}")
    private int maxAttempts;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public TransferOtpSession createTransferOtpSession(
            User user,
            InitiateTransferRequest request,
            BigDecimal fee,
            BigDecimal billedAmount
    ) {
        String sessionId = "OTP-SES-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        String otpCode = String.format("%06d", secureRandom.nextInt(1000000));
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(otpExpirationSeconds);

        TransferOtpSession session = TransferOtpSession.builder()
                .sessionId(sessionId)
                .username(user.getUsername())
                .sourceAccountNumber(request.getSourceAccountNumber().trim())
                .destinationAccountNumber(request.getDestinationAccountNumber().trim())
                .amount(request.getAmount())
                .transactionFee(fee)
                .billedAmount(billedAmount)
                .description(request.getDescription())
                .transactionReference(request.getTransactionReference())
                .otpCode(otpCode)
                .expiresAt(expiresAt)
                .attempts(0)
                .status(OtpSessionStatus.PENDING)
                .build();

        TransferOtpSession savedSession = otpSessionRepository.save(session);
        log.info("Created OTP session {} for user {} expiring at {}", sessionId, user.getUsername(), expiresAt);

        // Dispatch OTP notification
        notificationService.sendTransferOtp(user.getUsername(), request.getSourceAccountNumber(), otpCode, otpExpirationSeconds);

        return savedSession;
    }

    @Override
    @Transactional
    public TransferOtpSession validateAndConsumeOtp(String sessionId, String otpCode, String username) {
        log.info("Validating OTP for session: {} by user: {}", sessionId, username);

        TransferOtpSession session = otpSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new InvalidTransferException("Invalid transfer session ID: " + sessionId));

        if (username != null && !session.getUsername().equalsIgnoreCase(username)) {
            throw new InvalidTransferException("Unauthorized: Transfer session does not belong to the authenticated user");
        }

        if (session.getStatus() != OtpSessionStatus.PENDING) {
            throw new InvalidTransferException("Transfer session is already " + session.getStatus() + ". Please initiate a new transfer.");
        }

        if (session.isExpired()) {
            session.setStatus(OtpSessionStatus.EXPIRED);
            otpSessionRepository.save(session);
            throw new InvalidTransferException("OTP code has expired. Please initiate a new transfer.");
        }

        if (session.getAttempts() >= maxAttempts) {
            session.setStatus(OtpSessionStatus.FAILED);
            otpSessionRepository.save(session);
            throw new InvalidTransferException("Maximum verification attempts exceeded. Transfer session locked.");
        }

        if (!session.getOtpCode().equals(otpCode.trim())) {
            session.incrementAttempts();
            int remaining = maxAttempts - session.getAttempts();
            if (remaining <= 0) {
                session.setStatus(OtpSessionStatus.FAILED);
                otpSessionRepository.save(session);
                throw new InvalidTransferException("Invalid OTP code. Maximum verification attempts exceeded. Session locked.");
            }
            otpSessionRepository.save(session);
            throw new InvalidTransferException("Invalid OTP code. Remaining attempts: " + remaining);
        }

        // OTP verification passed
        session.setStatus(OtpSessionStatus.COMPLETED);
        TransferOtpSession verifiedSession = otpSessionRepository.save(session);
        log.info("OTP verification successful for session: {}", sessionId);

        return verifiedSession;
    }
}
