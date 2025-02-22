package com.processmonitor;

import com.processmonitor.dto.AlertResponse;
import com.processmonitor.dto.PageResponse;
import com.processmonitor.model.Alert;
import com.processmonitor.model.Job;
import com.processmonitor.model.enums.AlertSeverity;
import com.processmonitor.model.enums.JobType;
import com.processmonitor.repository.AlertRepository;
import com.processmonitor.service.AlertService;
import com.processmonitor.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AlertService alertService;

    private UUID jobId;
    private UUID alertId;
    private UUID userId;
    private Alert testAlert;

    @BeforeEach
    void setUp() {
        jobId = UUID.randomUUID();
        alertId = UUID.randomUUID();
        userId = UUID.randomUUID();
        testAlert = Alert.builder()
                .id(alertId)
                .jobId(jobId)
                .triggeredAt(LocalDateTime.now())
                .reason("Job execution failed")
                .severity(AlertSeverity.ERROR)
                .acknowledged(false)
                .build();
    }

    @Test
    void createAlert_savesAlertWithCorrectData() {
        when(alertRepository.save(any(Alert.class))).thenReturn(testAlert);

        Alert result = alertService.createAlert(jobId, "Job failed", AlertSeverity.ERROR);

        assertThat(result).isNotNull();
        assertThat(result.getJobId()).isEqualTo(jobId);
        assertThat(result.getSeverity()).isEqualTo(AlertSeverity.ERROR);
        verify(alertRepository).save(any(Alert.class));
    }

    @Test
    void createAlertWithNotification_triggersNotification() {
        Job job = Job.builder().id(jobId).name("Test Job")
                .type(JobType.HTTP_CALL).build();
        when(alertRepository.save(any(Alert.class))).thenReturn(testAlert);
        doNothing().when(notificationService).sendNotification(any(Job.class), any(Alert.class));

        Alert result = alertService.createAlertWithNotification(job, "Job failed", AlertSeverity.ERROR);

        assertThat(result).isNotNull();
        verify(notificationService).sendNotification(eq(job), any(Alert.class));
    }

    @Test
    void listUnacknowledged_returnsPaginatedResults() {
        Page<Alert> alertPage = new PageImpl<>(List.of(testAlert));
        when(alertRepository.findByAcknowledgedFalse(any())).thenReturn(alertPage);

        PageResponse<AlertResponse> response = alertService.listUnacknowledged(PageRequest.of(0, 20));

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent().get(0).getAcknowledged()).isFalse();
    }

    @Test
    void acknowledge_unacknowledgedAlert_setsAcknowledgedData() {
        when(alertRepository.findById(alertId)).thenReturn(Optional.of(testAlert));
        when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

        AlertResponse response = alertService.acknowledge(alertId, userId);

        assertThat(response.getAcknowledged()).isTrue();
        assertThat(response.getAcknowledgedBy()).isEqualTo(userId);
        assertThat(response.getAcknowledgedAt()).isNotNull();
    }

    @Test
    void acknowledge_alreadyAcknowledged_throwsException() {
        testAlert.setAcknowledged(true);
        when(alertRepository.findById(alertId)).thenReturn(Optional.of(testAlert));

        assertThatThrownBy(() -> alertService.acknowledge(alertId, userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already acknowledged");
    }

    @Test
    void acknowledge_alertNotFound_throwsException() {
        when(alertRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertService.acknowledge(UUID.randomUUID(), userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Alert not found");
    }

    @Test
    void countUnacknowledged_returnsCorrectCount() {
        when(alertRepository.countByAcknowledgedFalse()).thenReturn(5L);

        long count = alertService.countUnacknowledged();

        assertThat(count).isEqualTo(5L);
    }

    @Test
    void getById_existingAlert_returnsResponse() {
        when(alertRepository.findById(alertId)).thenReturn(Optional.of(testAlert));

        AlertResponse response = alertService.getById(alertId);

        assertThat(response.getId()).isEqualTo(alertId);
        assertThat(response.getJobId()).isEqualTo(jobId);
    }
}
