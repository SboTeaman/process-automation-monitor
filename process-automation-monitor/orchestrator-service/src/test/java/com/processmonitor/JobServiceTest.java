package com.processmonitor;

import com.processmonitor.dto.JobRequest;
import com.processmonitor.dto.JobResponse;
import com.processmonitor.dto.PageResponse;
import com.processmonitor.model.Job;
import com.processmonitor.model.enums.JobStatus;
import com.processmonitor.model.enums.JobType;
import com.processmonitor.repository.JobRepository;
import com.processmonitor.scheduler.JobScheduler;
import com.processmonitor.service.JobService;
import com.processmonitor.service.WorkerClientService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobScheduler jobScheduler;

    @Mock
    private WorkerClientService workerClientService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private JobService jobService;

    private final ObjectMapper mapper = new ObjectMapper();
    private UUID userId;
    private UUID jobId;
    private Job testJob;

    private JsonNode json(String raw) {
        try { return mapper.readTree(raw); } catch (Exception e) { return null; }
    }

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        jobId = UUID.randomUUID();
        testJob = Job.builder()
                .id(jobId)
                .name("Test Job")
                .type(JobType.HTTP_CALL)
                .config("{\"url\":\"https://example.com\"}")
                .schedule("0 8 * * MON")
                .timezone("UTC")
                .enabled(true)
                .deleted(false)
                .timeout(30)
                .maxRetries(3)
                .createdBy(userId)
                .lastStatus(JobStatus.PENDING)
                .build();
    }

    @Test
    void createJob_validRequest_returnsJobResponse() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"url\":\"https://example.com\"}");
        JobRequest request = new JobRequest("Test Job", JobType.HTTP_CALL,
                json("{\"url\":\"https://example.com\"}"), "0 8 * * MON",
                "UTC", true, 30, 3, null, null);

        when(jobRepository.save(any(Job.class))).thenReturn(testJob);

        JobResponse response = jobService.createJob(request, userId);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Test Job");
        assertThat(response.getType()).isEqualTo(JobType.HTTP_CALL);
        verify(jobRepository).save(any(Job.class));
        verify(jobScheduler).scheduleJob(any(Job.class));
    }

    @Test
    void createJob_invalidCronExpression_throwsException() {
        JobRequest request = new JobRequest("Test Job", JobType.HTTP_CALL,
                null, "invalid-cron", "UTC", true, 30, 3, null, null);

        assertThatThrownBy(() -> jobService.createJob(request, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid cron expression");
    }

    @Test
    void createJob_nullSchedule_doesNotScheduleQuartz() {
        JobRequest request = new JobRequest("Test Job", JobType.HTTP_CALL,
                null, null, "UTC", true, 30, 3, null, null);

        Job jobNoSchedule = Job.builder().id(jobId).name("Test Job")
                .type(JobType.HTTP_CALL).enabled(true).deleted(false)
                .createdBy(userId).lastStatus(JobStatus.PENDING).build();
        when(jobRepository.save(any(Job.class))).thenReturn(jobNoSchedule);

        jobService.createJob(request, userId);

        verify(jobScheduler, never()).scheduleJob(any(Job.class));
    }

    @Test
    void updateJob_asAdmin_updatesSuccessfully() {
        JobRequest request = new JobRequest("Updated Job", JobType.CSV_PROCESS,
                null, null, "UTC", true, 60, 2, null, null);

        when(jobRepository.findByIdAndDeletedFalse(jobId)).thenReturn(Optional.of(testJob));
        when(jobRepository.save(any(Job.class))).thenReturn(testJob);

        JobResponse response = jobService.updateJob(jobId, request, UUID.randomUUID(), "ADMIN");

        assertThat(response).isNotNull();
        verify(jobRepository).save(any(Job.class));
        verify(jobScheduler).rescheduleJob(any(Job.class));
    }

    @Test
    void updateJob_asOperatorOwnJob_updatesSuccessfully() {
        JobRequest request = new JobRequest("Updated Job", JobType.HTTP_CALL,
                null, null, "UTC", true, 30, 3, null, null);

        when(jobRepository.findByIdAndDeletedFalse(jobId)).thenReturn(Optional.of(testJob));
        when(jobRepository.save(any(Job.class))).thenReturn(testJob);

        JobResponse response = jobService.updateJob(jobId, request, userId, "OPERATOR");

        assertThat(response).isNotNull();
    }

    @Test
    void updateJob_asOperatorOtherJob_throwsSecurityException() {
        UUID otherUser = UUID.randomUUID();
        JobRequest request = new JobRequest("Updated Job", JobType.HTTP_CALL,
                null, null, "UTC", true, 30, 3, null, null);

        when(jobRepository.findByIdAndDeletedFalse(jobId)).thenReturn(Optional.of(testJob));

        assertThatThrownBy(() -> jobService.updateJob(jobId, request, otherUser, "OPERATOR"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void deleteJob_softDeletesAndUnschedules() {
        when(jobRepository.findByIdAndDeletedFalse(jobId)).thenReturn(Optional.of(testJob));
        when(jobRepository.save(any(Job.class))).thenReturn(testJob);

        jobService.deleteJob(jobId, userId, "ADMIN");

        verify(jobRepository).save(argThat(job -> job.getDeleted() && job.getDeletedAt() != null));
        verify(jobScheduler).unscheduleJob(jobId);
    }

    @Test
    void deleteJob_notFound_throwsException() {
        when(jobRepository.findByIdAndDeletedFalse(jobId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.deleteJob(jobId, userId, "ADMIN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Job not found");
    }

    @Test
    void listJobs_noFilters_returnsAllJobs() {
        Page<Job> jobPage = new PageImpl<>(List.of(testJob));
        when(jobRepository.findByDeletedFalse(any(PageRequest.class))).thenReturn(jobPage);

        PageResponse<JobResponse> response = jobService.listJobs(null, null, null, PageRequest.of(0, 20));

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    void listJobs_withTypeFilter_callsCorrectRepository() {
        Page<Job> jobPage = new PageImpl<>(List.of(testJob));
        when(jobRepository.findByTypeAndDeletedFalse(eq(JobType.HTTP_CALL), any())).thenReturn(jobPage);

        PageResponse<JobResponse> response = jobService.listJobs(JobType.HTTP_CALL, null, null, PageRequest.of(0, 20));

        assertThat(response.getContent()).hasSize(1);
        verify(jobRepository).findByTypeAndDeletedFalse(eq(JobType.HTTP_CALL), any());
    }

    @Test
    void toggleJob_enabledToDisabled_disablesAndUnschedules() {
        when(jobRepository.findByIdAndDeletedFalse(jobId)).thenReturn(Optional.of(testJob));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        JobResponse response = jobService.toggleJob(jobId, userId, "ADMIN");

        assertThat(response.getEnabled()).isFalse();
        verify(jobScheduler).unscheduleJob(jobId);
    }

    @Test
    void triggerJob_validJob_callsWorkerClient() {
        when(jobRepository.findByIdAndDeletedFalse(jobId)).thenReturn(Optional.of(testJob));
        doNothing().when(workerClientService).executeJob(any(Job.class), anyString());

        jobService.triggerJob(jobId, "correlation-id-123");

        verify(workerClientService).executeJob(eq(testJob), eq("correlation-id-123"));
    }

    @Test
    void getJobById_existingJob_returnsResponse() {
        when(jobRepository.findByIdAndDeletedFalse(jobId)).thenReturn(Optional.of(testJob));

        JobResponse response = jobService.getJobById(jobId);

        assertThat(response.getId()).isEqualTo(jobId);
        assertThat(response.getName()).isEqualTo("Test Job");
    }

    @Test
    void getJobById_notFound_throwsException() {
        when(jobRepository.findByIdAndDeletedFalse(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.getJobById(UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Job not found");
    }

    @Test
    void createJob_sanitizesConfig() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"url\":\"https://example.com\",\"js\":\"<script>alert(1)</script>\"}");
        JobRequest request = new JobRequest("Test Job", JobType.HTTP_CALL,
                json("{\"url\":\"https://example.com\",\"js\":\"<script>alert(1)</script>\"}"),
                null, "UTC", true, 30, 3, null, null);

        Job savedJob = Job.builder().id(jobId).name("Test Job").type(JobType.HTTP_CALL)
                .enabled(true).deleted(false).createdBy(userId).lastStatus(JobStatus.PENDING).build();
        when(jobRepository.save(any(Job.class))).thenReturn(savedJob);

        jobService.createJob(request, userId);

        verify(jobRepository).save(argThat(job ->
                job.getConfig() == null || !job.getConfig().contains("<script>")));
    }

    @Test
    void listJobs_withAllFilters_callsCorrectRepository() {
        UUID createdBy = UUID.randomUUID();
        Page<Job> jobPage = new PageImpl<>(List.of(testJob));
        when(jobRepository.findByTypeAndLastStatusAndCreatedByAndDeletedFalse(
                eq(JobType.HTTP_CALL), eq(JobStatus.SUCCESS), eq(createdBy), any()))
                .thenReturn(jobPage);

        PageResponse<JobResponse> response = jobService.listJobs(
                JobType.HTTP_CALL, JobStatus.SUCCESS, createdBy, PageRequest.of(0, 20));

        assertThat(response.getContent()).hasSize(1);
        verify(jobRepository).findByTypeAndLastStatusAndCreatedByAndDeletedFalse(
                eq(JobType.HTTP_CALL), eq(JobStatus.SUCCESS), eq(createdBy), any());
    }
}
