package com.dotlabs.moneytransfer.scheduler;

import com.dotlabs.moneytransfer.service.CommissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommissionAnalysisScheduler {

    private final CommissionService commissionService;

    /**
     * Scheduled job running at the configured cron schedule (default: 01:00 AM daily).
     * Protected by ShedLock to ensure only ONE instance executes in a multi-pod Kubernetes cluster.
     */
    @Scheduled(cron = "${app.scheduling.commission-cron:0 0 1 * * ?}")
    @SchedulerLock(name = "CommissionAnalysisJob", lockAtMostFor = "15m", lockAtLeastFor = "30s")
    public void executeCommissionAnalysis() {
        log.info("ShedLock acquired: Starting scheduled Commission Analysis job...");
        try {
            commissionService.runCommissionAnalysis();
            log.info("Scheduled Commission Analysis job completed successfully.");
        } catch (Exception e) {
            log.error("Error executing scheduled Commission Analysis job: ", e);
        }
    }
}
