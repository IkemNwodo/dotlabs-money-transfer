package com.dotlabs.moneytransfer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_transaction_summaries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyTransactionSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate summaryDate;

    @Column(nullable = false)
    private long totalTransactions;

    @Column(nullable = false)
    private long successfulTransactions;

    @Column(nullable = false)
    private long failedTransactions;

    @Column(nullable = false)
    private long insufficientFundsTransactions;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalVolume;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalFees;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalCommission;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
