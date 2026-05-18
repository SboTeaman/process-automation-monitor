package com.processmonitor;

import com.processmonitor.dto.ChangePasswordRequest;
import com.processmonitor.dto.LoginRequest;
import com.processmonitor.dto.LoginResponse;
import com.processmonitor.dto.RegisterRequest;
import com.processmonitor.model.User;
import com.processmonitor.model.enums.UserRole;
import com.processmonitor.repository.UserRepository;
import com.processmonitor.security.JwtUtil;
import com.processmonitor.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserService userService;

    private UUID userId;
    private User testUser;
    private final String rawPassword = "securePassword123";
    private final String hashedPassword = "$2a$12$hashedPasswordHash";

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = User.builder()
                .id(userId)
                .email("user@example.com")
                .passwordHash(hashedPassword)
                .role(UserRole.OPERATOR)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void register_newEmail_createsUser() {
        RegisterRequest request = new RegisterRequest("newuser@example.com", rawPassword);

        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(hashedPassword);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userService.register(request);

        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode(rawPassword);
    }

    @Test
    void register_duplicateEmail_throwsException() {
        RegisterRequest request = new RegisterRequest("user@example.com", rawPassword);
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    void register_passwordHashedWithBCrypt() {
        RegisterRequest request = new RegisterRequest("new@example.com", rawPassword);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(hashedPassword);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.register(request);

        verify(passwordEncoder).encode(rawPassword);
    }

    @Test
    void login_validCredentials_returnsTokens() {
        LoginRequest request = new LoginRequest("user@example.com", rawPassword);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(rawPassword, hashedPassword)).thenReturn(true);
        when(jwtUtil.generateAccessToken(anyString(), anyString())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(anyString(), anyString())).thenReturn("refresh-token");
        when(jwtUtil.getAccessTokenExpiry()).thenReturn(900000L);

        LoginResponse response = userService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void login_wrongPassword_throwsException() {
        LoginRequest request = new LoginRequest("user@example.com", "wrongpassword");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", hashedPassword)).thenReturn(false);

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void login_userNotFound_throwsException() {
        LoginRequest request = new LoginRequest("unknown@example.com", rawPassword);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void changePassword_correctCurrentPassword_updatesHash() {
        ChangePasswordRequest request = new ChangePasswordRequest(rawPassword, "newPassword456");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(rawPassword, hashedPassword)).thenReturn(true);
        when(passwordEncoder.encode("newPassword456")).thenReturn("newHashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.changePassword(userId, request);

        verify(userRepository).save(argThat(u -> "newHashedPassword".equals(u.getPasswordHash())));
    }

    @Test
    void changePassword_wrongCurrentPassword_throwsException() {
        ChangePasswordRequest request = new ChangePasswordRequest("wrongCurrent", "newPassword456");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongCurrent", hashedPassword)).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Current password is incorrect");
    }

    @Test
    void getProfile_existingUser_returnsUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        User result = userService.getProfile(userId);

        assertThat(result.getEmail()).isEqualTo("user@example.com");
        assertThat(result.getRole()).isEqualTo(UserRole.OPERATOR);
    }
}
