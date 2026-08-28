package com.dotlabs.moneytransfer.service;

import com.dotlabs.moneytransfer.dto.response.DailySummaryResponse;
import com.dotlabs.moneytransfer.entity.DailyTransactionSummary;

import java.time.LocalDate;

public interface SummaryService {
    DailySummaryResponse getDailySummary(LocalDate date);
    DailyTransactionSummary generateAndPersistDailySummary(LocalDate date);
}
