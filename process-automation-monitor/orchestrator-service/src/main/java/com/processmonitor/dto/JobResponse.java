package com.processmonitor.dto;

import com.processmonitor.model.Job;
import com.processmonitor.model.enums.JobStatus;
import com.processmonitor.model.enums.JobType;
import com.processmonitor.model.enums.NotificationChannel;
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
public class JobResponse {

    private UUID id;
    private String name;
    private JobType type;
    private String config;
    private String schedule;
    private String timezone;
    private Boolean enabled;
    private Integer timeout;
    private Integer maxRetries;
    private NotificationChannel notificationChannel;
    private String notificationTarget;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private LocalDateTime lastRunAt;
    private JobStatus lastStatus;

    public static JobResponse fromJob(Job job) {
        return JobResponse.builder()
                .id(job.getId())
                .name(job.getName())
                .type(job.getType())
                .config(job.getConfig())
                .schedule(job.getSchedule())
                .timezone(job.getTimezone())
                .enabled(job.getEnabled())
                .timeout(job.getTimeout())
                .maxRetries(job.getMaxRetries())
                .notificationChannel(job.getNotificationChannel())
                .notificationTarget(job.getNotificationTarget())
                .createdBy(job.getCreatedBy())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .deletedAt(job.getDeletedAt())
                .lastRunAt(job.getLastRunAt())
                .lastStatus(job.getLastStatus())
                .build();
    }
}
