package com.dotlabs.moneytransfer.entity;

import com.dotlabs.moneytransfer.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String transactionReference;

    @Column(nullable = false, length = 32)
    private String sourceAccountNumber;

    @Column(nullable = false, length = 32)
    private String destinationAccountNumber;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal transactionFee;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal billedAmount;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TransactionStatus status;

    @Column(length = 255)
    private String statusMessage;

    @Column(name = "commission_worthy")
    private Boolean commissionWorthy;

    @Column(precision = 19, scale = 4)
    private BigDecimal commission;

    @Column(name = "commission_processed_at")
    private LocalDateTime commissionProcessedAt;

    @CreationTimestamp
    @Column(name = "date_created", nullable = false, updatable = false)
    private LocalDateTime dateCreated;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
