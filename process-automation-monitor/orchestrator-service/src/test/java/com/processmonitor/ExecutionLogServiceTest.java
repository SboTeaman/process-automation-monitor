package com.processmonitor;

import com.processmonitor.dto.ExecutionLogResponse;
import com.processmonitor.dto.PageResponse;
import com.processmonitor.model.ExecutionLog;
import com.processmonitor.model.enums.JobStatus;
import com.processmonitor.repository.ExecutionLogRepository;
import com.processmonitor.service.ExecutionLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecutionLogServiceTest {

    @Mock
    private ExecutionLogRepository executionLogRepository;

    @InjectMocks
    private ExecutionLogService executionLogService;

    private UUID jobId;
    private UUID logId;
    private ExecutionLog testLog;

    @BeforeEach
    void setUp() {
        jobId = UUID.randomUUID();
        logId = UUID.randomUUID();
        testLog = ExecutionLog.builder()
                .id(logId)
                .jobId(jobId)
                .startedAt(LocalDateTime.now())
                .status(JobStatus.RUNNING)
                .attempt(1)
                .correlationId("test-correlation-id")
                .build();

        ReflectionTestUtils.setField(executionLogService, "retentionDays", 90);
    }

    @Test
    void createLog_createsRunningLog() {
        when(executionLogRepository.save(any(ExecutionLog.class))).thenReturn(testLog);

        ExecutionLog result = executionLogService.createLog(jobId, "correlation-id");

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(JobStatus.RUNNING);
        verify(executionLogRepository).save(any(ExecutionLog.class));
    }

    @Test
    void updateStatus_success_updatesFinishedAt() {
        when(executionLogRepository.findById(logId)).thenReturn(Optional.of(testLog));
        when(executionLogRepository.save(any(ExecutionLog.class))).thenAnswer(inv -> inv.getArgument(0));

        ExecutionLog result = executionLogService.updateStatus(logId, JobStatus.SUCCESS, "output", null);

        assertThat(result.getStatus()).isEqualTo(JobStatus.SUCCESS);
        assertThat(result.getFinishedAt()).isNotNull();
        assertThat(result.getOutput()).isEqualTo("output");
    }

    @Test
    void updateStatus_failed_setsErrorMessage() {
        when(executionLogRepository.findById(logId)).thenReturn(Optional.of(testLog));
        when(executionLogRepository.save(any(ExecutionLog.class))).thenAnswer(inv -> inv.getArgument(0));

        ExecutionLog result = executionLogService.updateStatus(logId, JobStatus.FAILED, null, "Connection failed");

        assertThat(result.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(result.getErrorMessage()).isEqualTo("Connection failed");
    }

    @Test
    void updateStatus_logNotFound_throwsException() {
        when(executionLogRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> executionLogService.updateStatus(UUID.randomUUID(), JobStatus.SUCCESS, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Execution log not found");
    }

    @Test
    void listByJobId_noFilters_returnsPagedResults() {
        Page<ExecutionLog> page = new PageImpl<>(List.of(testLog));
        when(executionLogRepository.findByJobId(eq(jobId), any())).thenReturn(page);

        PageResponse<ExecutionLogResponse> response = executionLogService.listByJobId(
                jobId, null, null, null, PageRequest.of(0, 20));

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    void listAll_withStatusFilter_callsCorrectRepository() {
        Page<ExecutionLog> page = new PageImpl<>(List.of(testLog));
        when(executionLogRepository.findByStatus(eq(JobStatus.FAILED), any())).thenReturn(page);

        PageResponse<ExecutionLogResponse> response = executionLogService.listAll(
                JobStatus.FAILED, null, null, PageRequest.of(0, 20));

        assertThat(response.getContent()).hasSize(1);
        verify(executionLogRepository).findByStatus(eq(JobStatus.FAILED), any());
    }

    @Test
    void cleanupOldLogs_deletesLogsBeyondRetention() {
        when(executionLogRepository.deleteByStartedAtBefore(any(LocalDateTime.class))).thenReturn(42);

        int deleted = executionLogService.cleanupOldLogs();

        assertThat(deleted).isEqualTo(42);
        verify(executionLogRepository).deleteByStartedAtBefore(any(LocalDateTime.class));
    }

    @Test
    void getById_existingLog_returnsResponse() {
        when(executionLogRepository.findById(logId)).thenReturn(Optional.of(testLog));

        ExecutionLogResponse response = executionLogService.getById(logId);

        assertThat(response.getId()).isEqualTo(logId);
        assertThat(response.getJobId()).isEqualTo(jobId);
        assertThat(response.getStatus()).isEqualTo(JobStatus.RUNNING);
    }
}
