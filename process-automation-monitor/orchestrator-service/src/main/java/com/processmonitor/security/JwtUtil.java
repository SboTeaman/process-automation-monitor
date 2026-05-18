package com.processmonitor.security;

import com.processmonitor.model.RevokedToken;
import com.processmonitor.repository.RevokedTokenRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtUtil {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-expiry:900000}")
    private long accessTokenExpiry;

    @Value("${app.jwt.refresh-token-expiry:604800000}")
    private long refreshTokenExpiry;

    private final RevokedTokenRepository revokedTokenRepository;

    private SecretKey getSigningKey() {
        // Use the secret bytes directly — no double-encoding.
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String userId, String role) {
        return buildToken(userId, role, TYPE_ACCESS, accessTokenExpiry);
    }

    public String generateRefreshToken(String userId, String role) {
        return buildToken(userId, role, TYPE_REFRESH, refreshTokenExpiry);
    }

    private String buildToken(String userId, String role, String type, long expiry) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expiry);
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId)
                .claims(Map.of(CLAIM_USER_ID, userId, CLAIM_ROLE, role, CLAIM_TYPE, type))
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired");
            return false;
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token");
            return false;
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token");
            return false;
        } catch (SecurityException e) {
            log.warn("Invalid JWT signature");
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("JWT token is empty or null");
            return false;
        }

        // Check revocation in the database (survives restarts and multi-instance deployments).
        if (revokedTokenRepository.existsByTokenHash(sha256(token))) {
            log.warn("Attempted use of revoked token");
            return false;
        }

        return true;
    }

    public String extractUserId(String token) {
        return getClaims(token).get(CLAIM_USER_ID, String.class);
    }

    public String extractRole(String token) {
        return getClaims(token).get(CLAIM_ROLE, String.class);
    }

    public String extractType(String token) {
        return getClaims(token).get(CLAIM_TYPE, String.class);
    }

    public boolean isAccessToken(String token) {
        return TYPE_ACCESS.equals(extractType(token));
    }

    public boolean isRefreshToken(String token) {
        return TYPE_REFRESH.equals(extractType(token));
    }

    public void revokeToken(String token) {
        Date expiration;
        try {
            expiration = getClaims(token).getExpiration();
        } catch (Exception e) {
            // Token is already invalid; nothing to store.
            return;
        }
        RevokedToken entry = RevokedToken.builder()
                .tokenHash(sha256(token))
                .revokedAt(Instant.now())
                .expiresAt(expiration.toInstant())
                .build();
        revokedTokenRepository.save(entry);
        log.info("Token revoked and persisted to database");
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getAccessTokenExpiry() {
        return accessTokenExpiry;
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
