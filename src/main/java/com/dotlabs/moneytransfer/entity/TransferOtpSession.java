package com.dotlabs.moneytransfer.entity;

import com.dotlabs.moneytransfer.enums.OtpSessionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transfer_otp_sessions", indexes = {
    @Index(name = "idx_otp_session_id", columnList = "sessionId", unique = true),
    @Index(name = "idx_otp_username", columnList = "username"),
    @Index(name = "idx_otp_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferOtpSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String sessionId;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(name = "source_account_number", nullable = false, length = 32)
    private String sourceAccountNumber;

    @Column(name = "destination_account_number", nullable = false, length = 32)
    private String destinationAccountNumber;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "transaction_fee", nullable = false, precision = 19, scale = 4)
    private BigDecimal transactionFee;

    @Column(name = "billed_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal billedAmount;

    @Column(length = 255)
    private String description;

    @Column(name = "transaction_reference", length = 64)
    private String transactionReference;

    @Column(name = "otp_code", nullable = false, length = 6)
    private String otpCode;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private int attempts = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private OtpSessionStatus status = OtpSessionStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public void incrementAttempts() {
        this.attempts++;
    }
}
