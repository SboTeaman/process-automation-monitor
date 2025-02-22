package com.processmonitor.dto;

import com.processmonitor.model.ExecutionLog;
import com.processmonitor.model.enums.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionLogResponse {

    private UUID id;
    private UUID jobId;
    private String jobName;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime executedAt;
    private JobStatus status;
    private String output;
    private String errorMessage;
    private String error;
    private Long durationMs;
    private Integer attempt;
    private String correlationId;

    public static ExecutionLogResponse fromLog(ExecutionLog log) {
        return fromLog(log, null);
    }

    public static ExecutionLogResponse fromLog(ExecutionLog log, String jobName) {
        Long durationMs = null;
        if (log.getStartedAt() != null && log.getFinishedAt() != null) {
            durationMs = ChronoUnit.MILLIS.between(log.getStartedAt(), log.getFinishedAt());
        }
        return ExecutionLogResponse.builder()
                .id(log.getId())
                .jobId(log.getJobId())
                .jobName(jobName)
                .startedAt(log.getStartedAt())
                .finishedAt(log.getFinishedAt())
                .executedAt(log.getStartedAt())
                .status(log.getStatus())
                .output(log.getOutput())
                .errorMessage(log.getErrorMessage())
                .error(log.getErrorMessage())
                .durationMs(durationMs)
                .attempt(log.getAttempt())
                .correlationId(log.getCorrelationId())
                .build();
    }
}
