package com.processmonitor.controller;

import com.processmonitor.dto.ErrorResponse;
import com.processmonitor.dto.ExecutionLogResponse;
import com.processmonitor.dto.PageResponse;
import com.processmonitor.filter.CorrelationIdFilter;
import com.processmonitor.model.enums.JobStatus;
import com.processmonitor.service.ExecutionLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Executions", description = "Execution log endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ExecutionController {

    private final ExecutionLogService executionLogService;

    @GetMapping("/jobs/{jobId}/executions")
    @Operation(summary = "Get execution logs for a specific job")
    public ResponseEntity<PageResponse<ExecutionLogResponse>> getJobExecutions(
            @PathVariable UUID jobId,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("startedAt").descending());
        PageResponse<ExecutionLogResponse> response = executionLogService.listByJobId(jobId, status, from, to, pageable);

        return ResponseEntity.ok()
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, getCorrelationId(request))
                .body(response);
    }

    @GetMapping("/executions")
    @Operation(summary = "Get all execution logs with filtering")
    public ResponseEntity<PageResponse<ExecutionLogResponse>> getAllExecutions(
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("startedAt").descending());
        PageResponse<ExecutionLogResponse> response = executionLogService.listAll(status, from, to, pageable);

        return ResponseEntity.ok()
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, getCorrelationId(request))
                .body(response);
    }

    @GetMapping("/executions/{id}")
    @Operation(summary = "Get execution log by ID")
    public ResponseEntity<?> getExecution(@PathVariable UUID id, HttpServletRequest request) {
        String correlationId = getCorrelationId(request);
        try {
            ExecutionLogResponse response = executionLogService.getById(id);
            return ResponseEntity.ok()
                    .header(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId)
                    .body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.of(e.getMessage(), "EXECUTION_LOG_NOT_FOUND", correlationId));
        }
    }

    private String getCorrelationId(HttpServletRequest request) {
        Object attr = request.getAttribute(CorrelationIdFilter.CORRELATION_ID_HEADER);
        return attr != null ? attr.toString() : UUID.randomUUID().toString();
    }
}
