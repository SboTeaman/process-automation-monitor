package com.processmonitor.controller;

import com.processmonitor.model.enums.JobStatus;
import com.processmonitor.repository.AlertRepository;
import com.processmonitor.repository.ExecutionLogRepository;
import com.processmonitor.repository.JobRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
@Tag(name = "Stats", description = "Dashboard statistics")
@SecurityRequirement(name = "bearerAuth")
public class StatsController {

    private final ExecutionLogRepository executionLogRepository;
    private final JobRepository jobRepository;
    private final AlertRepository alertRepository;

    @GetMapping("/summary")
    @Operation(summary = "Dashboard summary statistics (last 24h)")
    public ResponseEntity<Map<String, Object>> getSummary() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);

        long totalJobs = jobRepository.countByDeletedFalse();
        long successCount = executionLogRepository.countByStatusAndStartedAtAfter(JobStatus.SUCCESS, since);
        long failedCount = executionLogRepository.countByStatusAndStartedAtAfter(JobStatus.FAILED, since);
        long totalRecent = executionLogRepository.countByStartedAtAfter(since);
        long activeAlerts = alertRepository.countByAcknowledgedFalse();

        double successRate = totalRecent > 0
                ? Math.round((successCount * 100.0 / totalRecent) * 10.0) / 10.0
                : 0.0;

        return ResponseEntity.ok(Map.of(
                "totalJobs", totalJobs,
                "successRate", successRate,
                "recentErrors", failedCount,
                "activeAlerts", activeAlerts
        ));
    }

    @GetMapping("/daily")
    @Operation(summary = "Daily execution counts for the last 30 days")
    public ResponseEntity<List<Map<String, Object>>> getDaily() {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<Object[]> rows = executionLogRepository.findDailyCountsSince(since);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(Map.of(
                    "date", row[0].toString(),
                    "count", ((Number) row[1]).longValue()
            ));
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/jobs/performance")
    @Operation(summary = "Per-job success rate and average duration")
    public ResponseEntity<List<Map<String, Object>>> getJobPerformance() {
        List<Object[]> rows = executionLogRepository.findJobPerformance();

        Map<java.util.UUID, String> jobNames = jobRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(
                        j -> j.getId(), j -> j.getName()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            java.util.UUID jobId = java.util.UUID.fromString(row[0].toString());
            long total = ((Number) row[1]).longValue();
            long successCount = ((Number) row[2]).longValue();
            double avgDuration = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
            double successRate = total > 0 ? Math.round((successCount * 100.0 / total) * 10.0) / 10.0 : 0.0;

            Map<String, Object> entry = new java.util.HashMap<>();
            entry.put("jobId", jobId.toString());
            entry.put("jobName", jobNames.getOrDefault(jobId, jobId.toString()));
            entry.put("successRate", successRate);
            entry.put("avgDuration", avgDuration);
            result.add(entry);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/top-failing")
    @Operation(summary = "Top 10 jobs by failure count")
    public ResponseEntity<List<Map<String, Object>>> getTopFailing() {
        List<Object[]> rows = executionLogRepository.findTopFailingJobs();

        Map<java.util.UUID, String> jobNames = jobRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(
                        j -> j.getId(), j -> j.getName()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            java.util.UUID jobId = java.util.UUID.fromString(row[0].toString());
            long failureCount = ((Number) row[1]).longValue();

            result.add(Map.of(
                    "jobId", jobId.toString(),
                    "jobName", jobNames.getOrDefault(jobId, jobId.toString()),
                    "failureCount", failureCount
            ));
        }
        return ResponseEntity.ok(result);
    }
}
