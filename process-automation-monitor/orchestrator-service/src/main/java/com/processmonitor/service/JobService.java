package com.processmonitor.service;

import com.processmonitor.dto.JobRequest;
import com.processmonitor.dto.JobResponse;
import com.processmonitor.dto.PageResponse;
import com.processmonitor.model.Job;
import com.processmonitor.model.enums.JobStatus;
import com.processmonitor.model.enums.JobType;
import com.processmonitor.model.enums.UserRole;
import com.processmonitor.repository.JobRepository;
import com.processmonitor.scheduler.JobScheduler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

    private final JobRepository jobRepository;
    private final JobScheduler jobScheduler;
    private final WorkerClientService workerClientService;
    private final ObjectMapper objectMapper;

    @Transactional
    public JobResponse createJob(JobRequest request, UUID userId) {
        validateCronExpression(request.getSchedule());
        String sanitizedConfig = sanitizeConfig(request.getConfig());

        Job job = Job.builder()
                .name(request.getName())
                .type(request.getType())
                .config(sanitizedConfig)
                .schedule(toQuartzCron(request.getSchedule()))
                .timezone(request.getTimezone() != null ? request.getTimezone() : "UTC")
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .timeout(request.getTimeout() != null ? request.getTimeout() : 30)
                .maxRetries(request.getMaxRetries() != null ? request.getMaxRetries() : 3)
                .notificationChannel(request.getNotificationChannel())
                .notificationTarget(request.getNotificationTarget())
                .createdBy(userId)
                .build();

        Job saved = jobRepository.save(job);
        log.info("Job created: {} by user: {}", saved.getId(), userId);

        if (saved.getEnabled() && saved.getSchedule() != null) {
            jobScheduler.scheduleJob(saved);
        }

        return JobResponse.fromJob(saved);
    }

    @Transactional
    public JobResponse updateJob(UUID jobId, JobRequest request, UUID userId, String userRole) {
        Job job = getJobOrThrow(jobId);
        checkOwnership(job, userId, userRole);
        validateCronExpression(request.getSchedule());

        String sanitizedConfig = sanitizeConfig(request.getConfig());

        job.setName(request.getName());
        job.setType(request.getType());
        job.setConfig(sanitizedConfig);
        job.setSchedule(toQuartzCron(request.getSchedule()));
        if (request.getTimezone() != null) job.setTimezone(request.getTimezone());
        if (request.getEnabled() != null) job.setEnabled(request.getEnabled());
        if (request.getTimeout() != null) job.setTimeout(request.getTimeout());
        if (request.getMaxRetries() != null) job.setMaxRetries(request.getMaxRetries());
        job.setNotificationChannel(request.getNotificationChannel());
        job.setNotificationTarget(request.getNotificationTarget());

        Job updated = jobRepository.save(job);
        log.info("Job updated: {} by user: {}", jobId, userId);

        jobScheduler.rescheduleJob(updated);
        return JobResponse.fromJob(updated);
    }

    @Transactional
    public void deleteJob(UUID jobId, UUID userId, String userRole) {
        Job job = getJobOrThrow(jobId);
        checkOwnership(job, userId, userRole);

        job.setDeleted(true);
        job.setDeletedAt(LocalDateTime.now());
        job.setEnabled(false);
        jobRepository.save(job);

        jobScheduler.unscheduleJob(jobId);
        log.info("Job soft-deleted: {} by user: {}", jobId, userId);
    }

    @Transactional
    public JobResponse toggleJob(UUID jobId, UUID userId, String userRole) {
        Job job = getJobOrThrow(jobId);
        checkOwnership(job, userId, userRole);

        job.setEnabled(!job.getEnabled());
        Job updated = jobRepository.save(job);
        log.info("Job toggled: {} enabled={} by user: {}", jobId, updated.getEnabled(), userId);

        if (updated.getEnabled() && updated.getSchedule() != null) {
            jobScheduler.scheduleJob(updated);
        } else {
            jobScheduler.unscheduleJob(jobId);
        }

        return JobResponse.fromJob(updated);
    }

    @Transactional(readOnly = true)
    public JobResponse getJobById(UUID jobId) {
        return JobResponse.fromJob(getJobOrThrow(jobId));
    }

    @Transactional(readOnly = true)
    public PageResponse<JobResponse> listJobs(JobType type, JobStatus status,
                                               UUID createdBy, Pageable pageable) {
        Page<Job> page;

        if (type != null && status != null && createdBy != null) {
            page = jobRepository.findByTypeAndLastStatusAndCreatedByAndDeletedFalse(type, status, createdBy, pageable);
        } else if (type != null && status != null) {
            page = jobRepository.findByTypeAndLastStatusAndDeletedFalse(type, status, pageable);
        } else if (type != null && createdBy != null) {
            page = jobRepository.findByTypeAndCreatedByAndDeletedFalse(type, createdBy, pageable);
        } else if (status != null && createdBy != null) {
            page = jobRepository.findByLastStatusAndCreatedByAndDeletedFalse(status, createdBy, pageable);
        } else if (type != null) {
            page = jobRepository.findByTypeAndDeletedFalse(type, pageable);
        } else if (status != null) {
            page = jobRepository.findByLastStatusAndDeletedFalse(status, pageable);
        } else if (createdBy != null) {
            page = jobRepository.findByCreatedByAndDeletedFalse(createdBy, pageable);
        } else {
            page = jobRepository.findByDeletedFalse(pageable);
        }

        return PageResponse.fromPage(page.map(JobResponse::fromJob));
    }

    public void triggerJob(UUID jobId, String correlationId) {
        Job job = getJobOrThrow(jobId);
        log.info("Manually triggering job: {} correlationId: {}", jobId, correlationId);
        workerClientService.executeJob(job, correlationId);
    }

    private Job getJobOrThrow(UUID jobId) {
        return jobRepository.findByIdAndDeletedFalse(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
    }

    private void checkOwnership(Job job, UUID userId, String userRole) {
        if (UserRole.ADMIN.name().equals(userRole)) return;
        if (!job.getCreatedBy().equals(userId)) {
            throw new SecurityException("Access denied: you can only modify your own jobs");
        }
    }

    static String toQuartzCron(String schedule) {
        if (schedule == null) return null;
        String trimmed = schedule.trim();
        String[] parts = trimmed.split("\\s+");
        if (parts.length == 5) {
            // Linux cron: min hour dom month dow → Quartz: sec min hour dom month dow
            // If dow is not '*', replace dom '*' with '?' (Quartz requirement)
            String sec = "0";
            String min = parts[0];
            String hour = parts[1];
            String dom = parts[2];
            String month = parts[3];
            String dow = parts[4];
            if (!dow.equals("*") && dom.equals("*")) {
                dom = "?";
            } else if (dow.equals("*") && !dom.equals("*")) {
                dow = "?";
            }
            return sec + " " + min + " " + hour + " " + dom + " " + month + " " + dow;
        }
        return trimmed;
    }

    private void validateCronExpression(String schedule) {
        if (schedule == null || schedule.isBlank()) return;
        String quartz = toQuartzCron(schedule);
        if (!org.quartz.CronExpression.isValidExpression(quartz)) {
            throw new IllegalArgumentException("Invalid cron expression: " + schedule
                    + " (use standard 5-field cron: min hour dom month dow)");
        }
    }

    private String sanitizeConfig(JsonNode config) {
        if (config == null) return null;
        try {
            String json = objectMapper.writeValueAsString(config);
            return json
                    .replaceAll("(?i)<script[^>]*>.*?</script>", "")
                    .replaceAll("(?i)javascript:", "")
                    .replaceAll("(?i)on\\w+\\s*=", "")
                    .trim();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid config JSON");
        }
    }
}
