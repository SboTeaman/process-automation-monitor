package com.processmonitor;

import com.processmonitor.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String userId = UUID.randomUUID().toString();
    private final String role = "OPERATOR";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret",
                "test-secret-key-for-testing-purposes-must-be-long-enough-for-hmac256");
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiry", 900000L);
        ReflectionTestUtils.setField(jwtUtil, "refreshTokenExpiry", 604800000L);
    }

    @Test
    void generateAccessToken_returnsNonNullToken() {
        String token = jwtUtil.generateAccessToken(userId, role);
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void generateRefreshToken_returnsNonNullToken() {
        String token = jwtUtil.generateRefreshToken(userId, role);
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void validateToken_validAccessToken_returnsTrue() {
        String token = jwtUtil.generateAccessToken(userId, role);
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_validRefreshToken_returnsTrue() {
        String token = jwtUtil.generateRefreshToken(userId, role);
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    void extractUserId_fromAccessToken_returnsCorrectId() {
        String token = jwtUtil.generateAccessToken(userId, role);
        String extracted = jwtUtil.extractUserId(token);
        assertThat(extracted).isEqualTo(userId);
    }

    @Test
    void extractRole_fromAccessToken_returnsCorrectRole() {
        String token = jwtUtil.generateAccessToken(userId, role);
        String extracted = jwtUtil.extractRole(token);
        assertThat(extracted).isEqualTo(role);
    }

    @Test
    void isAccessToken_accessToken_returnsTrue() {
        String token = jwtUtil.generateAccessToken(userId, role);
        assertThat(jwtUtil.isAccessToken(token)).isTrue();
    }

    @Test
    void isRefreshToken_refreshToken_returnsTrue() {
        String token = jwtUtil.generateRefreshToken(userId, role);
        assertThat(jwtUtil.isRefreshToken(token)).isTrue();
    }

    @Test
    void validateToken_revokedToken_returnsFalse() {
        String token = jwtUtil.generateAccessToken(userId, role);
        jwtUtil.revokeToken(token);
        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_invalidToken_returnsFalse() {
        assertThat(jwtUtil.validateToken("invalid.token.here")).isFalse();
    }

    @Test
    void validateToken_expiredToken_returnsFalse() {
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiry", -1000L);
        String token = jwtUtil.generateAccessToken(userId, role);
        assertThat(jwtUtil.validateToken(token)).isFalse();
    }
}
