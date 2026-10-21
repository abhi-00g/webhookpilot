package com.webhookpilot.service;

import com.webhookpilot.dto.request.LoginRequest;
import com.webhookpilot.dto.request.RegisterRequest;
import com.webhookpilot.dto.response.AuthResponse;
import com.webhookpilot.exception.DuplicateResourceException;
import com.webhookpilot.model.User;
import com.webhookpilot.repository.UserRepository;
import com.webhookpilot.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider tokenProvider;
    @InjectMocks private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User mockUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("Password1!");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("Password1!");

        mockUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .passwordHash("hashed_password")
                .build();
    }

    @Test
    @DisplayName("register creates user and returns JWT")
    void register_success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(mockUser);
        when(tokenProvider.generateToken(any(UUID.class))).thenReturn("jwt_token");

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("jwt_token", response.getToken());
        assertEquals("test@example.com", response.getEmail());
        verify(userRepository).saveAndFlush(any(User.class));
    }

    @Test
    @DisplayName("register throws DuplicateResourceException for existing email")
    void register_duplicateEmail() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> authService.register(registerRequest));

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("login returns JWT for valid credentials")
    void login_success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("Password1!", "hashed_password")).thenReturn(true);
        when(tokenProvider.generateToken(mockUser.getId())).thenReturn("jwt_token");

        AuthResponse response = authService.login(loginRequest);

        assertEquals("jwt_token", response.getToken());
        assertEquals("test@example.com", response.getEmail());
    }

    @Test
    @DisplayName("login throws BadCredentialsException for wrong email")
    void login_wrongEmail() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class,
                () -> authService.login(loginRequest));
    }

    @Test
    @DisplayName("login throws BadCredentialsException for wrong password")
    void login_wrongPassword() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("Password1!", "hashed_password")).thenReturn(false);

        assertThrows(BadCredentialsException.class,
                () -> authService.login(loginRequest));
    }

    @Test
    @DisplayName("register hashes password before saving")
    void register_hashesPassword() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("Password1!")).thenReturn("bcrypt_hash");
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(mockUser);
        when(tokenProvider.generateToken(any())).thenReturn("token");

        authService.register(registerRequest);

        verify(passwordEncoder).encode("Password1!");
        verify(userRepository).saveAndFlush(argThat(user ->
                "bcrypt_hash".equals(user.getPasswordHash())));
    }
}
