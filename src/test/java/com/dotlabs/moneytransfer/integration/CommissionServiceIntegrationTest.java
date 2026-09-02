package com.dotlabs.moneytransfer.integration;

import com.dotlabs.moneytransfer.dto.response.CommissionAnalysisResponse;
import com.dotlabs.moneytransfer.entity.Transaction;
import com.dotlabs.moneytransfer.enums.TransactionStatus;
import com.dotlabs.moneytransfer.repository.TransactionRepository;
import com.dotlabs.moneytransfer.service.CommissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CommissionServiceIntegrationTest {

    @Autowired
    private CommissionService commissionService;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
    }

    @Test
    @Transactional
    @DisplayName("End-to-End DB Bulk Update: Calculate commissions directly inside the database")
    void testCommissionAnalysisDirectlyInDatabase() {
        Transaction tx1 = Transaction.builder()
                .transactionReference("TX-COMM-1")
                .sourceAccountNumber("1000000001")
                .destinationAccountNumber("1000000002")
                .amount(new BigDecimal("1000.00"))
                .transactionFee(new BigDecimal("5.00")) // 20% = 1.00
                .billedAmount(new BigDecimal("1005.00"))
                .status(TransactionStatus.SUCCESSFUL)
                .commissionWorthy(null)
                .dateCreated(LocalDateTime.now())
                .build();

        Transaction tx2 = Transaction.builder()
                .transactionReference("TX-COMM-2")
                .sourceAccountNumber("1000000001")
                .destinationAccountNumber("1000000002")
                .amount(new BigDecimal("20000.00"))
                .transactionFee(new BigDecimal("100.00")) // 20% = 20.00
                .billedAmount(new BigDecimal("20100.00"))
                .status(TransactionStatus.SUCCESSFUL)
                .commissionWorthy(null)
                .dateCreated(LocalDateTime.now())
                .build();

        Transaction failedTx = Transaction.builder()
                .transactionReference("TX-COMM-3")
                .sourceAccountNumber("1000000001")
                .destinationAccountNumber("1000000002")
                .amount(new BigDecimal("5000.00"))
                .transactionFee(new BigDecimal("25.00"))
                .billedAmount(new BigDecimal("5025.00"))
                .status(TransactionStatus.INSUFFICIENT_FUNDS)
                .commissionWorthy(null)
                .dateCreated(LocalDateTime.now())
                .build();

        transactionRepository.save(tx1);
        transactionRepository.save(tx2);
        transactionRepository.save(failedTx);

        CommissionAnalysisResponse response = commissionService.runCommissionAnalysis();

        assertThat(response).isNotNull();
        assertThat(response.getTotalAnalyzed()).isEqualTo(3);
        assertThat(response.getCommissionWorthyUpdated()).isEqualTo(2);
        assertThat(response.getTotalCommissionCalculated()).isEqualByComparingTo("21.00");

        Transaction updatedTx1 = transactionRepository.findByTransactionReference("TX-COMM-1").orElseThrow();
        assertThat(updatedTx1.getCommissionWorthy()).isTrue();
        assertThat(updatedTx1.getCommission()).isEqualByComparingTo("1.0000");
        assertThat(updatedTx1.getCommissionProcessedAt()).isNotNull();

        Transaction updatedTx2 = transactionRepository.findByTransactionReference("TX-COMM-2").orElseThrow();
        assertThat(updatedTx2.getCommissionWorthy()).isTrue();
        assertThat(updatedTx2.getCommission()).isEqualByComparingTo("20.0000");

        Transaction updatedFailedTx = transactionRepository.findByTransactionReference("TX-COMM-3").orElseThrow();
        assertThat(updatedFailedTx.getCommissionWorthy()).isFalse();
        assertThat(updatedFailedTx.getCommission()).isEqualByComparingTo("0.0000");
    }
}
