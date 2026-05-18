package com.processmonitor.scheduler;

import com.processmonitor.service.ExecutionLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RetentionCleanupJob {

    private final ExecutionLogService executionLogService;

    // Run daily at 2:00 AM
    @Scheduled(cron = "0 0 2 * * *")
    public void runRetentionCleanup() {
        log.info("Starting daily retention cleanup");
        int deleted = executionLogService.cleanupOldLogs();
        log.info("Retention cleanup completed: deleted {} records", deleted);
    }
}
