package com.processmonitor.service;

import com.processmonitor.model.Alert;
import com.processmonitor.model.Job;
import com.processmonitor.model.enums.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;
    private final RestTemplate restTemplate;

    public void sendNotification(Job job, Alert alert) {
        if (job.getNotificationChannel() == null || job.getNotificationTarget() == null) {
            log.debug("No notification configured for job: {}", job.getId());
            return;
        }

        try {
            if (job.getNotificationChannel() == NotificationChannel.EMAIL) {
                sendEmail(job, alert);
            } else if (job.getNotificationChannel() == NotificationChannel.WEBHOOK) {
                sendWebhook(job, alert);
            }
        } catch (Exception e) {
            log.error("Failed to send notification for job: {} alert: {} error: {}",
                    job.getId(), alert.getId(), e.getMessage());
        }
    }

    private void sendEmail(Job job, Alert alert) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(job.getNotificationTarget());
            message.setSubject(String.format("[Process Monitor] Alert: Job '%s' - %s",
                    job.getName(), alert.getSeverity()));
            message.setText(String.format(
                    "Alert triggered for job: %s\nSeverity: %s\nReason: %s\nTriggered at: %s\nAlert ID: %s",
                    job.getName(),
                    alert.getSeverity(),
                    alert.getReason(),
                    alert.getTriggeredAt(),
                    alert.getId()
            ));

            mailSender.send(message);
            log.info("Email notification sent for job: {} to: {}", job.getId(), job.getNotificationTarget());
        } catch (Exception e) {
            log.error("Failed to send email notification for job: {}: {}", job.getId(), e.getMessage());
            throw e;
        }
    }

    private void sendWebhook(Job job, Alert alert) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("jobId", job.getId().toString());
            payload.put("jobName", job.getName());
            payload.put("alertId", alert.getId().toString());
            payload.put("severity", alert.getSeverity().name());
            payload.put("reason", alert.getReason());
            payload.put("triggeredAt", alert.getTriggeredAt().toString());

            restTemplate.postForEntity(job.getNotificationTarget(), payload, Void.class);
            log.info("Webhook notification sent for job: {} to: {}", job.getId(), job.getNotificationTarget());
        } catch (Exception e) {
            log.error("Failed to send webhook notification for job: {}: {}", job.getId(), e.getMessage());
            throw e;
        }
    }
}
