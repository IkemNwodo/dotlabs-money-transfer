package com.dotlabs.moneytransfer.repository;

import com.dotlabs.moneytransfer.entity.Transaction;
import com.dotlabs.moneytransfer.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByTransactionReference(String transactionReference);

    boolean existsByTransactionReference(String transactionReference);

    /**
     * Finds all successful transactions that have not yet had commission evaluated.
     */
    List<Transaction> findByStatusAndCommissionWorthyIsNull(TransactionStatus status);

    /**
     * Finds transactions within a datetime range (e.g. start of day to end of day).
     */
    List<Transaction> findByDateCreatedBetween(LocalDateTime startDateTime, LocalDateTime endDateTime);

    /**
     * Finds all transactions for a specific account (either as source or destination).
     */
    @Query("SELECT t FROM Transaction t WHERE t.sourceAccountNumber = :accountNumber OR t.destinationAccountNumber = :accountNumber")
    List<Transaction> findByAccountNumber(@Param("accountNumber") String accountNumber);

    /**
     * Aggregates total commission to be processed for unevaluated successful transactions directly in the database.
     */
    @Query("SELECT COALESCE(SUM(t.transactionFee * :rate), 0.0) FROM Transaction t WHERE t.status = :status AND t.commissionWorthy IS NULL")
    BigDecimal calculateTotalCommissionForPending(
            @Param("status") TransactionStatus status,
            @Param("rate") BigDecimal rate
    );

    /**
     * Bulk updates commission for all successful transactions directly in the database.
     * Calculates commission as (transactionFee * rate) inside the database.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Transaction t SET t.commissionWorthy = true, t.commission = (t.transactionFee * :rate), t.commissionProcessedAt = :processedAt WHERE t.status = :status AND t.commissionWorthy IS NULL")
    int evaluateCommissionForSuccessfulTransactions(
            @Param("status") TransactionStatus status,
            @Param("rate") BigDecimal rate,
            @Param("processedAt") LocalDateTime processedAt
    );

    /**
     * Bulk marks all non-successful transactions (INSUFFICIENT_FUNDS, FAILED) as non-commission-worthy in the database.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Transaction t SET t.commissionWorthy = false, t.commission = 0.0, t.commissionProcessedAt = :processedAt WHERE t.status <> :status AND t.commissionWorthy IS NULL")
    int markNonSuccessfulTransactionsNonCommissionWorthy(
            @Param("status") TransactionStatus status,
            @Param("processedAt") LocalDateTime processedAt
    );
}
