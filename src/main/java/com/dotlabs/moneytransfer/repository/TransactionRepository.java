package com.dotlabs.moneytransfer.repository;

import com.dotlabs.moneytransfer.entity.Transaction;
import com.dotlabs.moneytransfer.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
