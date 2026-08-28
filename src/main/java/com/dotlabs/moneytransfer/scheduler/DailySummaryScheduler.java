package com.dotlabs.moneytransfer.scheduler;

import com.dotlabs.moneytransfer.service.SummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailySummaryScheduler {

    private final SummaryService summaryService;

    /**
     * Scheduled job running at the configured cron schedule (default: 02:00 AM daily).
     * Protected by ShedLock to ensure single execution across all Kubernetes pods.
     */
    @Scheduled(cron = "${app.scheduling.summary-cron:0 0 2 * * ?}")
    @SchedulerLock(name = "DailySummaryJob", lockAtMostFor = "15m", lockAtLeastFor = "30s")
    public void executeDailySummaryGeneration() {
        log.info("ShedLock acquired: Starting scheduled Daily Summary generation job...");
        try {
            // Aggregate summary for the previous day
            LocalDate yesterday = LocalDate.now().minusDays(1);
            summaryService.generateAndPersistDailySummary(yesterday);
            log.info("Scheduled Daily Summary job completed successfully for date: {}", yesterday);
        } catch (Exception e) {
            log.error("Error executing scheduled Daily Summary job: ", e);
        }
    }
}
