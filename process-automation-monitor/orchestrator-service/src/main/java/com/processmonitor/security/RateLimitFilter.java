package com.processmonitor.security;

import com.processmonitor.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Comma-separated list of trusted reverse-proxy IPs.
     * Only when the direct connection comes from one of these IPs will
     * X-Forwarded-For be consulted. Leave empty (default) to always use
     * the direct TCP remote address — safe for deployments without a proxy.
     */
    @Value("${app.security.trusted-proxies:}")
    private String trustedProxiesRaw;

    private Set<String> getTrustedProxies() {
        if (trustedProxiesRaw == null || trustedProxiesRaw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(trustedProxiesRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (isRateLimitedPath(path)) {
            String clientIp = getClientIp(request);
            Bucket bucket = buckets.computeIfAbsent(clientIp, this::createBucket);

            if (!bucket.tryConsume(1)) {
                log.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, path);
                sendRateLimitResponse(request, response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRateLimitedPath(String path) {
        return path.equals("/auth/login") || path.equals("/auth/register");
    }

    private Bucket createBucket(String ip) {
        Refill refill = Refill.greedy(10, Duration.ofMinutes(1));
        Bandwidth limit = Bandwidth.classic(10, refill);
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Returns the real client IP.
     *
     * X-Forwarded-For is only trusted when the direct TCP connection arrives
     * from a known trusted proxy. If not, the raw remote address is used,
     * preventing clients from spoofing their IP to bypass rate limiting.
     */
    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        Set<String> trustedProxies = getTrustedProxies();

        if (!trustedProxies.isEmpty() && trustedProxies.contains(remoteAddr)) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                return xForwardedFor.split(",")[0].trim();
            }
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isBlank()) {
                return xRealIp;
            }
        }

        return remoteAddr;
    }

    private void sendRateLimitResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String correlationId = (String) request.getAttribute("X-Correlation-Id");
        if (correlationId == null) correlationId = "unknown";

        ErrorResponse error = ErrorResponse.of(
                "Too many requests. Please try again later.",
                "RATE_LIMIT_EXCEEDED",
                correlationId
        );

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.findAndRegisterModules();
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
