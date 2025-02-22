package com.processmonitor.service;

import com.processmonitor.model.ExecutionLog;
import com.processmonitor.model.Job;
import com.processmonitor.model.enums.AlertSeverity;
import com.processmonitor.model.enums.JobStatus;
import com.processmonitor.repository.JobRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkerClientService {

    private static final String EXECUTE_PATH = "/execute";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String WORKER_API_KEY_HEADER = "X-Worker-Api-Key";

    private final RestTemplate restTemplate;
    private final JobRepository jobRepository;
    private final ExecutionLogService executionLogService;
    private final AlertService alertService;
    private final ObjectMapper objectMapper;

    @Value("${app.worker.base-url:http://worker-service:8000}")
    private String workerBaseUrl;

    @Value("${app.worker.api-key}")
    private String workerApiKey;

    @Async("jobExecutor")
    public void executeJob(Job job, String correlationId) {
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }

        ExecutionLog executionLog = executionLogService.createLog(job.getId(), correlationId);

        updateJobRunStatus(job, JobStatus.RUNNING);

        int attempt = 1;
        int maxRetries = job.getMaxRetries() != null ? job.getMaxRetries() : 3;
        long[] backoffSeconds = {5, 15, 30};

        while (attempt <= maxRetries + 1) {
            try {
                WorkerResponse response = callWorker(job, correlationId);

                if (response != null && "SUCCESS".equalsIgnoreCase(response.getStatus())) {
                    executionLogService.updateStatus(executionLog.getId(), JobStatus.SUCCESS,
                            response.getOutput(), null);
                    updateJobRunStatus(job, JobStatus.SUCCESS);
                    log.info("Job executed successfully: {} correlationId: {}", job.getId(), correlationId);
                    return;
                } else {
                    String errorMsg = response != null ? response.getError() : "No response from worker";
                    if (attempt <= maxRetries) {
                        log.warn("Job attempt {} failed for: {}, retrying...", attempt, job.getId());
                        sleepBackoff(backoffSeconds, attempt - 1);
                        attempt++;
                        executionLog = executionLogService.createLogWithAttempt(job.getId(), correlationId, attempt);
                    } else {
                        handleJobFailure(job, executionLog, errorMsg, correlationId);
                        return;
                    }
                }
            } catch (RestClientException e) {
                log.error("Worker call failed for job: {} attempt: {} error: {}", job.getId(), attempt, e.getMessage());
                if (attempt <= maxRetries) {
                    sleepBackoff(backoffSeconds, attempt - 1);
                    attempt++;
                    executionLog = executionLogService.createLogWithAttempt(job.getId(), correlationId, attempt);
                } else {
                    handleJobFailure(job, executionLog, "Worker service unavailable: " + e.getMessage(), correlationId);
                    return;
                }
            } catch (Exception e) {
                log.error("Unexpected error executing job: {} error: {}", job.getId(), e.getMessage());
                handleJobFailure(job, executionLog, "Unexpected error: " + e.getMessage(), correlationId);
                return;
            }
        }
    }

    private WorkerResponse callWorker(Job job, String correlationId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(CORRELATION_ID_HEADER, correlationId);
        headers.set(WORKER_API_KEY_HEADER, workerApiKey);

        Map<String, Object> configMap;
        try {
            configMap = objectMapper.readValue(job.getConfig(), new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Could not parse job config as JSON, sending as-is: {}", e.getMessage());
            configMap = new HashMap<>();
        }

        Map<String, Object> body = new HashMap<>();
        body.put("job_id", job.getId().toString());
        body.put("job_type", job.getType().name());
        body.put("config", configMap);
        body.put("timeout", job.getTimeout() != null ? job.getTimeout() : 30);
        body.put("max_retries", 0);
        body.put("correlation_id", correlationId);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<WorkerResponse> response = restTemplate.exchange(
                workerBaseUrl + EXECUTE_PATH,
                HttpMethod.POST,
                request,
                WorkerResponse.class
        );

        return response.getBody();
    }

    private void handleJobFailure(Job job, ExecutionLog executionLog, String errorMsg, String correlationId) {
        executionLogService.updateStatus(executionLog.getId(), JobStatus.FAILED, null, errorMsg);
        updateJobRunStatus(job, JobStatus.FAILED);

        log.error("Job failed after all retries: {} correlationId: {} error: {}",
                job.getId(), correlationId, errorMsg);

        alertService.createAlertWithNotification(job,
                "Job failed: " + errorMsg,
                AlertSeverity.ERROR);
    }

    private void updateJobRunStatus(Job job, JobStatus status) {
        job.setLastStatus(status);
        job.setLastRunAt(LocalDateTime.now());
        jobRepository.save(job);
    }

    private void sleepBackoff(long[] backoffSeconds, int index) {
        try {
            long seconds = index < backoffSeconds.length ? backoffSeconds[index] : 30;
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Backoff sleep interrupted");
        }
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class WorkerResponse {
        private String status;
        @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.fasterxml.jackson.databind.deser.std.JsonNodeDeserializer.class)
        private com.fasterxml.jackson.databind.JsonNode output;
        @com.fasterxml.jackson.annotation.JsonProperty("error_message")
        private String error;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getOutput() { return output != null ? output.toString() : null; }
        public void setOutput(com.fasterxml.jackson.databind.JsonNode output) { this.output = output; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}
