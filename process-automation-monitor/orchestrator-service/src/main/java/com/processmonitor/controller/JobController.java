package com.processmonitor.controller;

import com.processmonitor.dto.*;
import com.processmonitor.filter.CorrelationIdFilter;
import com.processmonitor.model.enums.JobStatus;
import com.processmonitor.model.enums.JobType;
import com.processmonitor.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Jobs", description = "Job management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class JobController {

    private final JobService jobService;

    @GetMapping
    @Operation(summary = "List all jobs with pagination and filtering")
    public ResponseEntity<PageResponse<JobResponse>> listJobs(
            @RequestParam(required = false) JobType type,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) UUID createdBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            HttpServletRequest request) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        PageRequest pageable = PageRequest.of(page, size, sort);
        PageResponse<JobResponse> response = jobService.listJobs(type, status, createdBy, pageable);

        return ResponseEntity.ok()
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, getCorrelationId(request))
                .body(response);
    }

    @PostMapping
    @Operation(summary = "Create a new job")
    public ResponseEntity<?> createJob(@Valid @RequestBody JobRequest jobRequest,
                                        Authentication auth,
                                        HttpServletRequest request) {
        String correlationId = getCorrelationId(request);
        try {
            UUID userId = UUID.fromString(auth.getName());
            JobResponse response = jobService.createJob(jobRequest, userId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .header(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId)
                    .body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.of(e.getMessage(), "INVALID_JOB_DATA", correlationId));
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get job by ID")
    public ResponseEntity<?> getJob(@PathVariable UUID id, HttpServletRequest request) {
        String correlationId = getCorrelationId(request);
        try {
            JobResponse response = jobService.getJobById(id);
            return ResponseEntity.ok()
                    .header(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId)
                    .body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.of(e.getMessage(), "JOB_NOT_FOUND", correlationId));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a job")
    public ResponseEntity<?> updateJob(@PathVariable UUID id,
                                        @Valid @RequestBody JobRequest jobRequest,
                                        Authentication auth,
                                        HttpServletRequest request) {
        String correlationId = getCorrelationId(request);
        try {
            UUID userId = UUID.fromString(auth.getName());
            String role = extractRole(auth);
            JobResponse response = jobService.updateJob(id, jobRequest, userId, role);
            return ResponseEntity.ok()
                    .header(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId)
                    .body(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ErrorResponse.of(e.getMessage(), "ACCESS_DENIED", correlationId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.of(e.getMessage(), "JOB_NOT_FOUND", correlationId));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete a job")
    public ResponseEntity<?> deleteJob(@PathVariable UUID id,
                                        Authentication auth,
                                        HttpServletRequest request) {
        String correlationId = getCorrelationId(request);
        try {
            UUID userId = UUID.fromString(auth.getName());
            String role = extractRole(auth);
            jobService.deleteJob(id, userId, role);
            return ResponseEntity.noContent()
                    .header(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId)
                    .build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ErrorResponse.of(e.getMessage(), "ACCESS_DENIED", correlationId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.of(e.getMessage(), "JOB_NOT_FOUND", correlationId));
        }
    }

    @PostMapping("/{id}/trigger")
    @Operation(summary = "Manually trigger a job")
    public ResponseEntity<?> triggerJob(@PathVariable UUID id,
                                         HttpServletRequest request) {
        String correlationId = getCorrelationId(request);
        try {
            jobService.triggerJob(id, correlationId);
            return ResponseEntity.accepted()
                    .header(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId)
                    .body(java.util.Map.of("message", "Job triggered", "correlationId", correlationId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.of(e.getMessage(), "JOB_NOT_FOUND", correlationId));
        }
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Toggle job enabled/disabled")
    public ResponseEntity<?> toggleJob(@PathVariable UUID id,
                                        Authentication auth,
                                        HttpServletRequest request) {
        String correlationId = getCorrelationId(request);
        try {
            UUID userId = UUID.fromString(auth.getName());
            String role = extractRole(auth);
            JobResponse response = jobService.toggleJob(id, userId, role);
            return ResponseEntity.ok()
                    .header(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId)
                    .body(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ErrorResponse.of(e.getMessage(), "ACCESS_DENIED", correlationId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.of(e.getMessage(), "JOB_NOT_FOUND", correlationId));
        }
    }

    private String getCorrelationId(HttpServletRequest request) {
        Object attr = request.getAttribute(CorrelationIdFilter.CORRELATION_ID_HEADER);
        return attr != null ? attr.toString() : UUID.randomUUID().toString();
    }

    private String extractRole(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst()
                .orElse("OPERATOR");
    }
}
