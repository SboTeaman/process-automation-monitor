package com.processmonitor.controller;

import com.processmonitor.dto.*;
import com.processmonitor.filter.CorrelationIdFilter;
import com.processmonitor.model.User;
import com.processmonitor.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Auth endpoints")
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request,
                                       HttpServletRequest httpRequest) {
        String correlationId = getCorrelationId(httpRequest);
        try {
            User user = userService.register(request);
            log.info("User registered: {} correlationId: {}", user.getEmail(), correlationId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .header(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId)
                    .body(java.util.Map.of(
                            "userId", user.getId().toString(),
                            "email", user.getEmail(),
                            "role", user.getRole().name()
                    ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ErrorResponse.of(e.getMessage(), "EMAIL_ALREADY_EXISTS", correlationId));
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive JWT tokens")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                    HttpServletRequest httpRequest) {
        String correlationId = getCorrelationId(httpRequest);
        try {
            LoginResponse response = userService.login(request);
            return ResponseEntity.ok()
                    .header(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId)
                    .body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.of(e.getMessage(), "INVALID_CREDENTIALS", correlationId));
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshTokenRequest request,
                                      HttpServletRequest httpRequest) {
        String correlationId = getCorrelationId(httpRequest);
        try {
            LoginResponse response = userService.refreshToken(request.getRefreshToken());
            return ResponseEntity.ok()
                    .header(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId)
                    .body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.of(e.getMessage(), "INVALID_REFRESH_TOKEN", correlationId));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and revoke token")
    public ResponseEntity<?> logout(HttpServletRequest httpRequest) {
        String correlationId = getCorrelationId(httpRequest);
        String authHeader = httpRequest.getHeader("Authorization");
        String token = null;

        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        userService.logout(token);
        return ResponseEntity.ok()
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId)
                .body(java.util.Map.of("message", "Logged out successfully"));
    }

    private String getCorrelationId(HttpServletRequest request) {
        Object attr = request.getAttribute(CorrelationIdFilter.CORRELATION_ID_HEADER);
        return attr != null ? attr.toString() : java.util.UUID.randomUUID().toString();
    }
}
