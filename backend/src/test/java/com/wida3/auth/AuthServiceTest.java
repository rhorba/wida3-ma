package com.wida3.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wida3.auth.dto.LoginRequest;
import com.wida3.auth.dto.RegisterRequest;
import com.wida3.auth.entity.Role;
import com.wida3.auth.entity.User;
import com.wida3.auth.exception.AccountLockedException;
import com.wida3.auth.exception.BreachedPasswordException;
import com.wida3.auth.exception.EmailAlreadyRegisteredException;
import com.wida3.auth.exception.InvalidCredentialsException;
import com.wida3.auth.repository.RoleRepository;
import com.wida3.auth.repository.UserRepository;
import com.wida3.auth.security.JwtService;
import com.wida3.auth.security.PasswordBreachChecker;
import com.wida3.auth.service.AuthService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;
    private PasswordBreachChecker breachChecker;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        breachChecker = mock(PasswordBreachChecker.class);
        jwtService = mock(JwtService.class);
        authService = new AuthService(userRepository, roleRepository, passwordEncoder, breachChecker, jwtService, 5, 15);
    }

    @Test
    void register_emailAlreadyRegistered_throws() {
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);
        RegisterRequest request = new RegisterRequest("taken@example.com", "correcthorsebattery", "Name", null);

        assertThatThrownBy(() -> authService.register(request)).isInstanceOf(EmailAlreadyRegisteredException.class);
    }

    @Test
    void register_breachedPassword_throws() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(breachChecker.isBreached("password123!")).thenReturn(true);
        RegisterRequest request = new RegisterRequest("new@example.com", "password123!", "Name", null);

        assertThatThrownBy(() -> authService.register(request)).isInstanceOf(BreachedPasswordException.class);
    }

    @Test
    void register_success_issuesToken() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(breachChecker.isBreached(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(roleRepository.findByName("RENTER")).thenReturn(Optional.of(new Role("RENTER")));
        when(jwtService.issueAccessToken(any(), any())).thenReturn("token");

        RegisterRequest request = new RegisterRequest("new@example.com", "correcthorsebattery", "Name", null);
        var response = authService.register(request);

        assertThat(response.accessToken()).isEqualTo("token");
        verify(userRepository).save(any());
    }

    @Test
    void login_wrongPassword_incrementsFailedAttempts() {
        User user = new User("user@example.com", "hashed", "Name", null);
        user.addRole(new Role("RENTER"));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        LoginRequest request = new LoginRequest("user@example.com", "wrong");

        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(InvalidCredentialsException.class);
        assertThat(user.getFailedAttempts()).isEqualTo((short) 1);
    }

    @Test
    void login_fifthFailedAttempt_locksAccount() {
        User user = new User("user@example.com", "hashed", "Name", null);
        user.setFailedAttempts((short) 4);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        LoginRequest request = new LoginRequest("user@example.com", "wrong");

        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(InvalidCredentialsException.class);
        assertThat(user.getFailedAttempts()).isEqualTo((short) 5);
        assertThat(user.getLockedUntil()).isNotNull();
    }

    @Test
    void login_lockedAccount_rejectsEvenWithCorrectPassword() {
        User user = new User("user@example.com", "hashed", "Name", null);
        user.setLockedUntil(Instant.now().plusSeconds(600));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest("user@example.com", "correcthorsebattery");

        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(AccountLockedException.class);
    }

    @Test
    void login_success_resetsFailedAttempts() {
        User user = new User("user@example.com", "hashed", "Name", null);
        user.setFailedAttempts((short) 3);
        user.addRole(new Role("RENTER"));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        when(jwtService.issueAccessToken(any(), any())).thenReturn("token");

        LoginRequest request = new LoginRequest("user@example.com", "correcthorsebattery");
        var response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("token");
        assertThat(user.getFailedAttempts()).isEqualTo((short) 0);
    }
}
