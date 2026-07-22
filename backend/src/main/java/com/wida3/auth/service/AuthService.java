package com.wida3.auth.service;

import com.wida3.auth.dto.AuthResponse;
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
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordBreachChecker breachChecker;
    private final JwtService jwtService;
    private final int maxFailedAttempts;
    private final long lockoutDurationMin;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            PasswordBreachChecker breachChecker,
            JwtService jwtService,
            @Value("${app.auth.max-failed-attempts}") int maxFailedAttempts,
            @Value("${app.auth.lockout-duration-min}") long lockoutDurationMin) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.breachChecker = breachChecker;
        this.jwtService = jwtService;
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockoutDurationMin = lockoutDurationMin;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyRegisteredException();
        }
        if (breachChecker.isBreached(request.password())) {
            throw new BreachedPasswordException();
        }

        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.fullName(),
                request.phone());

        Role renterRole = roleRepository.findByName("RENTER")
                .orElseThrow(() -> new IllegalStateException("RENTER role not seeded"));
        user.addRole(renterRole);

        userRepository.save(user);

        Set<String> roleNames = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        String accessToken = jwtService.issueAccessToken(user.getEmail(), roleNames);
        return new AuthResponse(accessToken, user.getEmail(), roleNames);
    }

    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (user.isLocked()) {
            throw new AccountLockedException(user.getLockedUntil());
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            registerFailedAttempt(user);
            throw new InvalidCredentialsException();
        }

        if (user.getFailedAttempts() > 0 || user.getLockedUntil() != null) {
            user.setFailedAttempts((short) 0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }

        Set<String> roleNames = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        String accessToken = jwtService.issueAccessToken(user.getEmail(), roleNames);
        return new AuthResponse(accessToken, user.getEmail(), roleNames);
    }

    private void registerFailedAttempt(User user) {
        short attempts = (short) (user.getFailedAttempts() + 1);
        user.setFailedAttempts(attempts);
        if (attempts >= maxFailedAttempts) {
            user.setLockedUntil(Instant.now().plusSeconds(lockoutDurationMin * 60));
        }
        userRepository.save(user);
    }
}
