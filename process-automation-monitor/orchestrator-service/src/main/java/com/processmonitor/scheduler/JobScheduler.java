package com.processmonitor.scheduler;

import com.processmonitor.model.Job;
import com.processmonitor.repository.JobRepository;
import com.processmonitor.service.WorkerClientService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobScheduler {

    private static final String JOB_GROUP = "processmonitor";
    private static final String TRIGGER_GROUP = "processmonitor-triggers";
    private static final String JOB_ID_KEY = "jobId";

    private final Scheduler quartzScheduler;
    private final JobRepository jobRepository;
    private final WorkerClientService workerClientService;

    @PostConstruct
    public void initializeSchedules() {
        List<Job> scheduledJobs = jobRepository.findAllScheduledJobs();
        log.info("Loading {} scheduled jobs on startup", scheduledJobs.size());
        scheduledJobs.forEach(job -> {
            try {
                scheduleJob(job);
            } catch (Exception e) {
                log.error("Failed to schedule job: {} error: {}", job.getId(), e.getMessage());
            }
        });
    }

    public void scheduleJob(Job job) {
        if (job.getSchedule() == null || job.getSchedule().isBlank()) {
            log.debug("Job {} has no cron schedule, skipping", job.getId());
            return;
        }

        try {
            String jobKey = job.getId().toString();
            JobDetail jobDetail = buildJobDetail(job, jobKey);
            Trigger trigger = buildTrigger(job, jobKey);

            if (quartzScheduler.checkExists(new JobKey(jobKey, JOB_GROUP))) {
                quartzScheduler.rescheduleJob(new TriggerKey(jobKey, TRIGGER_GROUP), trigger);
                log.info("Rescheduled job: {}", job.getId());
            } else {
                quartzScheduler.scheduleJob(jobDetail, trigger);
                log.info("Scheduled job: {} with cron: {}", job.getId(), job.getSchedule());
            }
        } catch (SchedulerException e) {
            log.error("Failed to schedule job: {} error: {}", job.getId(), e.getMessage());
        }
    }

    public void rescheduleJob(Job job) {
        unscheduleJob(job.getId());
        if (job.getEnabled() && job.getSchedule() != null) {
            scheduleJob(job);
        }
    }

    public void unscheduleJob(UUID jobId) {
        try {
            String key = jobId.toString();
            quartzScheduler.deleteJob(new JobKey(key, JOB_GROUP));
            log.info("Unscheduled job: {}", jobId);
        } catch (SchedulerException e) {
            log.error("Failed to unschedule job: {} error: {}", jobId, e.getMessage());
        }
    }

    public void executeNow(Job job, String correlationId) {
        workerClientService.executeJob(job, correlationId);
    }

    private JobDetail buildJobDetail(Job job, String key) {
        JobDataMap dataMap = new JobDataMap();
        dataMap.put(JOB_ID_KEY, job.getId().toString());

        return JobBuilder.newJob(QuartzJobRunner.class)
                .withIdentity(key, JOB_GROUP)
                .withDescription(job.getName())
                .usingJobData(dataMap)
                .storeDurably()
                .build();
    }

    private Trigger buildTrigger(Job job, String key) {
        String tz = job.getTimezone() != null ? job.getTimezone() : "UTC";
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder
                .cronSchedule(job.getSchedule())
                .inTimeZone(TimeZone.getTimeZone(tz))
                .withMisfireHandlingInstructionFireAndProceed();

        return TriggerBuilder.newTrigger()
                .withIdentity(key, TRIGGER_GROUP)
                .withSchedule(scheduleBuilder)
                .build();
    }
}
