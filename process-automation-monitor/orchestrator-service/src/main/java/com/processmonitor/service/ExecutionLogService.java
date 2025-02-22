package com.processmonitor.service;

import com.processmonitor.dto.ExecutionLogResponse;
import com.processmonitor.dto.PageResponse;
import com.processmonitor.model.ExecutionLog;
import com.processmonitor.model.enums.JobStatus;
import com.processmonitor.repository.ExecutionLogRepository;
import com.processmonitor.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExecutionLogService {

    private final ExecutionLogRepository executionLogRepository;
    private final JobRepository jobRepository;

    @Value("${app.retention.days:90}")
    private int retentionDays;

    @Transactional
    public ExecutionLog createLog(UUID jobId, String correlationId) {
        ExecutionLog executionLog = ExecutionLog.builder()
                .jobId(jobId)
                .startedAt(LocalDateTime.now())
                .status(JobStatus.RUNNING)
                .correlationId(correlationId)
                .attempt(1)
                .build();
        ExecutionLog saved = executionLogRepository.save(executionLog);
        log.info("Execution log created: {} for job: {} correlationId: {}", saved.getId(), jobId, correlationId);
        return saved;
    }

    @Transactional
    public ExecutionLog updateStatus(UUID logId, JobStatus status, String output, String errorMessage) {
        ExecutionLog executionLog = executionLogRepository.findById(logId)
                .orElseThrow(() -> new IllegalArgumentException("Execution log not found: " + logId));

        executionLog.setStatus(status);
        executionLog.setFinishedAt(LocalDateTime.now());
        if (output != null) executionLog.setOutput(output);
        if (errorMessage != null) executionLog.setErrorMessage(errorMessage);

        return executionLogRepository.save(executionLog);
    }

    @Transactional
    public ExecutionLog createLogWithAttempt(UUID jobId, String correlationId, int attempt) {
        ExecutionLog executionLog = ExecutionLog.builder()
                .jobId(jobId)
                .startedAt(LocalDateTime.now())
                .status(JobStatus.RUNNING)
                .correlationId(correlationId)
                .attempt(attempt)
                .build();
        return executionLogRepository.save(executionLog);
    }

    @Transactional(readOnly = true)
    public ExecutionLogResponse getById(UUID logId) {
        return executionLogRepository.findById(logId)
                .map(log -> ExecutionLogResponse.fromLog(log, resolveJobName(log.getJobId())))
                .orElseThrow(() -> new IllegalArgumentException("Execution log not found: " + logId));
    }

    @Transactional(readOnly = true)
    public PageResponse<ExecutionLogResponse> listByJobId(UUID jobId, JobStatus status,
                                                           LocalDateTime from, LocalDateTime to,
                                                           Pageable pageable) {
        Page<ExecutionLog> page;

        if (status != null && from != null && to != null) {
            page = executionLogRepository.findByJobIdAndStatusAndStartedAtBetween(jobId, status, from, to, pageable);
        } else if (from != null && to != null) {
            page = executionLogRepository.findByJobIdAndStartedAtBetween(jobId, from, to, pageable);
        } else if (status != null) {
            page = executionLogRepository.findByJobIdAndStatus(jobId, status, pageable);
        } else {
            page = executionLogRepository.findByJobId(jobId, pageable);
        }

        Map<UUID, String> names = buildJobNameCache(page);
        return PageResponse.fromPage(page.map(log -> ExecutionLogResponse.fromLog(log, names.get(log.getJobId()))));
    }

    @Transactional(readOnly = true)
    public PageResponse<ExecutionLogResponse> listAll(JobStatus status,
                                                       LocalDateTime from, LocalDateTime to,
                                                       Pageable pageable) {
        Page<ExecutionLog> page;

        if (status != null && from != null && to != null) {
            page = executionLogRepository.findByStatus(status, pageable);
        } else if (from != null && to != null) {
            page = executionLogRepository.findByStartedAtBetween(from, to, pageable);
        } else if (status != null) {
            page = executionLogRepository.findByStatus(status, pageable);
        } else {
            page = executionLogRepository.findAll(pageable);
        }

        Map<UUID, String> names = buildJobNameCache(page);
        return PageResponse.fromPage(page.map(log -> ExecutionLogResponse.fromLog(log, names.get(log.getJobId()))));
    }

    private Map<UUID, String> buildJobNameCache(Page<ExecutionLog> page) {
        Set<UUID> jobIds = page.map(ExecutionLog::getJobId).toSet();
        return jobRepository.findAllById(jobIds).stream()
                .collect(Collectors.toMap(j -> j.getId(), j -> j.getName()));
    }

    private String resolveJobName(UUID jobId) {
        return jobRepository.findById(jobId).map(j -> j.getName()).orElse(null);
    }

    @Transactional
    public int cleanupOldLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        int deleted = executionLogRepository.deleteByStartedAtBefore(cutoff);
        log.info("Retention cleanup: deleted {} execution logs older than {} days", deleted, retentionDays);
        return deleted;
    }
}
