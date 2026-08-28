package com.dotlabs.moneytransfer.repository;

import com.dotlabs.moneytransfer.entity.DailyTransactionSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyTransactionSummaryRepository extends JpaRepository<DailyTransactionSummary, Long> {

    Optional<DailyTransactionSummary> findBySummaryDate(LocalDate summaryDate);

    boolean existsBySummaryDate(LocalDate summaryDate);
}
