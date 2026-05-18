package com.processmonitor.model;

import com.processmonitor.model.enums.JobStatus;
import com.processmonitor.model.enums.JobType;
import com.processmonitor.model.enums.NotificationChannel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobType type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String config;

    private String schedule;

    @Builder.Default
    private String timezone = "UTC";

    @Builder.Default
    private Boolean enabled = true;

    @Builder.Default
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder.Default
    private Integer timeout = 30;

    @Column(name = "max_retries")
    @Builder.Default
    private Integer maxRetries = 3;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_channel")
    private NotificationChannel notificationChannel;

    @Column(name = "notification_target")
    private String notificationTarget;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_status")
    @Builder.Default
    private JobStatus lastStatus = JobStatus.PENDING;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (deleted == null) deleted = false;
        if (enabled == null) enabled = true;
        if (timeout == null) timeout = 30;
        if (maxRetries == null) maxRetries = 3;
        if (timezone == null) timezone = "UTC";
        if (lastStatus == null) lastStatus = JobStatus.PENDING;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
