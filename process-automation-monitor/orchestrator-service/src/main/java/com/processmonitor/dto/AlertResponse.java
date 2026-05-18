package com.processmonitor.dto;

import com.processmonitor.model.Alert;
import com.processmonitor.model.enums.AlertSeverity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponse {

    private UUID id;
    private UUID jobId;
    private String jobName;
    private LocalDateTime triggeredAt;
    private String reason;
    private AlertSeverity severity;
    private Boolean acknowledged;
    private LocalDateTime acknowledgedAt;
    private UUID acknowledgedBy;

    public static AlertResponse fromAlert(Alert alert) {
        return fromAlert(alert, null);
    }

    public static AlertResponse fromAlert(Alert alert, String jobName) {
        return AlertResponse.builder()
                .id(alert.getId())
                .jobId(alert.getJobId())
                .jobName(jobName)
                .triggeredAt(alert.getTriggeredAt())
                .reason(alert.getReason())
                .severity(alert.getSeverity())
                .acknowledged(alert.getAcknowledged())
                .acknowledgedAt(alert.getAcknowledgedAt())
                .acknowledgedBy(alert.getAcknowledgedBy())
                .build();
    }
}
