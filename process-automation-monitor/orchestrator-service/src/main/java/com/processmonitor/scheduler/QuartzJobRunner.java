package com.processmonitor.scheduler;

import com.processmonitor.model.Job;
import com.processmonitor.repository.JobRepository;
import com.processmonitor.service.WorkerClientService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
@DisallowConcurrentExecution
public class QuartzJobRunner extends QuartzJobBean {

    private JobRepository jobRepository;
    private WorkerClientService workerClientService;

    public void setJobRepository(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public void setWorkerClientService(WorkerClientService workerClientService) {
        this.workerClientService = workerClientService;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        String jobIdStr = context.getJobDetail().getJobDataMap().getString("jobId");
        log.info("Quartz trigger fired for job: {}", jobIdStr);

        try {
            UUID jobId = UUID.fromString(jobIdStr);
            Optional<Job> jobOpt = jobRepository.findByIdAndDeletedFalse(jobId);

            if (jobOpt.isEmpty()) {
                log.warn("Job not found or deleted: {}", jobId);
                return;
            }

            Job job = jobOpt.get();
            if (!job.getEnabled()) {
                log.info("Job is disabled, skipping execution: {}", jobId);
                return;
            }

            String correlationId = UUID.randomUUID().toString();
            workerClientService.executeJob(job, correlationId);
        } catch (Exception e) {
            log.error("Error executing scheduled job: {} error: {}", jobIdStr, e.getMessage());
            throw new JobExecutionException(e);
        }
    }
}
