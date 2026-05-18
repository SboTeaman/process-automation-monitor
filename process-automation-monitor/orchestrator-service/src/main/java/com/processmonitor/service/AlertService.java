package com.processmonitor.service;

import com.processmonitor.dto.AlertResponse;
import com.processmonitor.dto.PageResponse;
import com.processmonitor.model.Alert;
import com.processmonitor.model.Job;
import com.processmonitor.model.enums.AlertSeverity;
import com.processmonitor.repository.AlertRepository;
import com.processmonitor.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertRepository alertRepository;
    private final NotificationService notificationService;
    private final JobRepository jobRepository;

    @Transactional
    public Alert createAlert(UUID jobId, String reason, AlertSeverity severity) {
        Alert alert = Alert.builder()
                .jobId(jobId)
                .triggeredAt(LocalDateTime.now())
                .reason(reason)
                .severity(severity)
                .acknowledged(false)
                .build();

        Alert saved = alertRepository.save(alert);
        log.info("Alert created: {} for job: {} reason: {}", saved.getId(), jobId, reason);
        return saved;
    }

    @Transactional
    public Alert createAlertWithNotification(Job job, String reason, AlertSeverity severity) {
        Alert alert = createAlert(job.getId(), reason, severity);
        notificationService.sendNotification(job, alert);
        return alert;
    }

    @Transactional(readOnly = true)
    public PageResponse<AlertResponse> listUnacknowledged(Pageable pageable) {
        Page<Alert> page = alertRepository.findByAcknowledgedFalse(pageable);
        Set<UUID> jobIds = page.map(Alert::getJobId).toSet();
        Map<UUID, String> names = jobRepository.findAllById(jobIds).stream()
                .collect(Collectors.toMap(j -> j.getId(), j -> j.getName()));
        return PageResponse.fromPage(page.map(a -> AlertResponse.fromAlert(a, names.get(a.getJobId()))));
    }

    @Transactional
    public AlertResponse acknowledge(UUID alertId, UUID userId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));

        if (alert.getAcknowledged()) {
            throw new IllegalStateException("Alert already acknowledged");
        }

        alert.setAcknowledged(true);
        alert.setAcknowledgedAt(LocalDateTime.now());
        alert.setAcknowledgedBy(userId);

        Alert saved = alertRepository.save(alert);
        log.info("Alert acknowledged: {} by user: {}", alertId, userId);
        return AlertResponse.fromAlert(saved);
    }

    @Transactional(readOnly = true)
    public AlertResponse getById(UUID alertId) {
        return alertRepository.findById(alertId)
                .map(AlertResponse::fromAlert)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));
    }

    @Transactional(readOnly = true)
    public long countUnacknowledged() {
        return alertRepository.countByAcknowledgedFalse();
    }
}
