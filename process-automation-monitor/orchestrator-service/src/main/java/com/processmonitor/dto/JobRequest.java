package com.processmonitor.dto;

import com.processmonitor.model.enums.JobType;
import com.processmonitor.model.enums.NotificationChannel;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRequest {

    @NotBlank(message = "Job name is required")
    private String name;

    @NotNull(message = "Job type is required")
    private JobType type;

    private JsonNode config;

    private String schedule;

    @Builder.Default
    private String timezone = "UTC";

    @Builder.Default
    private Boolean enabled = true;

    @Min(value = 1, message = "Timeout must be at least 1 second")
    @Max(value = 300, message = "Timeout cannot exceed 300 seconds")
    @Builder.Default
    private Integer timeout = 30;

    @Min(value = 0, message = "Max retries cannot be negative")
    @Max(value = 10, message = "Max retries cannot exceed 10")
    @Builder.Default
    private Integer maxRetries = 3;

    private NotificationChannel notificationChannel;

    private String notificationTarget;
}
