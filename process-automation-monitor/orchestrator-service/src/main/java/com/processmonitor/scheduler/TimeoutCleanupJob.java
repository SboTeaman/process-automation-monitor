package com.processmonitor.scheduler;

import com.processmonitor.model.ExecutionLog;
import com.processmonitor.model.Job;
import com.processmonitor.model.enums.JobStatus;
import com.processmonitor.repository.ExecutionLogRepository;
import com.processmonitor.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class TimeoutCleanupJob {

    private final ExecutionLogRepository executionLogRepository;
    private final JobRepository jobRepository;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void cleanupTimedOutExecutions() {
        log.debug("Running timeout cleanup job");

        List<ExecutionLog> runningLogs = executionLogRepository
                .findByStatusAndStartedAtBefore(JobStatus.RUNNING, LocalDateTime.now().minusSeconds(1));

        int cleaned = 0;
        for (ExecutionLog executionLog : runningLogs) {
            Optional<Job> jobOpt = jobRepository.findByIdAndDeletedFalse(executionLog.getJobId());
            if (jobOpt.isEmpty()) continue;

            Job job = jobOpt.get();
            int timeout = job.getTimeout() != null ? job.getTimeout() : 30;
            int maxRetries = job.getMaxRetries() != null ? job.getMaxRetries() : 3;
            long maxSeconds = (long) timeout * (maxRetries + 1);

            LocalDateTime cutoff = executionLog.getStartedAt().plusSeconds(maxSeconds);
            if (LocalDateTime.now().isAfter(cutoff)) {
                executionLog.setStatus(JobStatus.FAILED);
                executionLog.setFinishedAt(LocalDateTime.now());
                executionLog.setErrorMessage("Execution timed out after " + maxSeconds + " seconds");
                executionLogRepository.save(executionLog);

                job.setLastStatus(JobStatus.FAILED);
                jobRepository.save(job);
                cleaned++;
            }
        }

        if (cleaned > 0) {
            log.info("Timeout cleanup: marked {} executions as FAILED", cleaned);
        }
    }
}
