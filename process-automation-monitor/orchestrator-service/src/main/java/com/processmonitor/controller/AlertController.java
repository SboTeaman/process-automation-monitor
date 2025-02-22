package com.processmonitor.controller;

import com.processmonitor.dto.AlertResponse;
import com.processmonitor.dto.ErrorResponse;
import com.processmonitor.dto.PageResponse;
import com.processmonitor.filter.CorrelationIdFilter;
import com.processmonitor.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/alerts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Alerts", description = "Alert management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    @Operation(summary = "List all unacknowledged alerts with pagination")
    public ResponseEntity<PageResponse<AlertResponse>> listAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("triggeredAt").descending());
        PageResponse<AlertResponse> response = alertService.listUnacknowledged(pageable);

        return ResponseEntity.ok()
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, getCorrelationId(request))
                .body(response);
    }

    @PostMapping("/{id}/acknowledge")
    @Operation(summary = "Acknowledge an alert")
    public ResponseEntity<?> acknowledgeAlert(@PathVariable UUID id,
                                               Authentication auth,
                                               HttpServletRequest request) {
        String correlationId = getCorrelationId(request);
        try {
            UUID userId = UUID.fromString(auth.getName());
            AlertResponse response = alertService.acknowledge(id, userId);
            return ResponseEntity.ok()
                    .header(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId)
                    .body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.of(e.getMessage(), "ALERT_NOT_FOUND", correlationId));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ErrorResponse.of(e.getMessage(), "ALERT_ALREADY_ACKNOWLEDGED", correlationId));
        }
    }

    private String getCorrelationId(HttpServletRequest request) {
        Object attr = request.getAttribute(CorrelationIdFilter.CORRELATION_ID_HEADER);
        return attr != null ? attr.toString() : UUID.randomUUID().toString();
    }
}
