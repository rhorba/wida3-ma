package com.wida3.auth.service;

import com.wida3.auth.dto.AuthResponse;
import com.wida3.auth.dto.LoginRequest;
import com.wida3.auth.dto.RegisterRequest;
import com.wida3.auth.dto.TokenPair;
import com.wida3.auth.entity.Role;
import com.wida3.auth.entity.User;
import com.wida3.auth.exception.AccountLockedException;
import com.wida3.auth.exception.BreachedPasswordException;
import com.wida3.auth.exception.EmailAlreadyRegisteredException;
import com.wida3.auth.exception.InvalidCredentialsException;
import com.wida3.auth.exception.InvalidRoleRequestException;
import com.wida3.auth.repository.RoleRepository;
import com.wida3.auth.repository.UserRepository;
import com.wida3.auth.security.JwtService;
import com.wida3.auth.security.PasswordBreachChecker;
import com.wida3.auth.security.RefreshTokenService;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Set<String> SELF_ASSIGNABLE_ROLES = Set.of("OWNER", "RENTER");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordBreachChecker breachChecker;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final int maxFailedAttempts;
    private final long lockoutDurationMin;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            PasswordBreachChecker breachChecker,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            @Value("${app.auth.max-failed-attempts}") int maxFailedAttempts,
            @Value("${app.auth.lockout-duration-min}") long lockoutDurationMin) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.breachChecker = breachChecker;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockoutDurationMin = lockoutDurationMin;
    }

    @Transactional
    public TokenPair register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyRegisteredException();
        }
        if (breachChecker.isBreached(request.password())) {
            throw new BreachedPasswordException();
        }

        for (String requested : request.roles()) {
            if (!SELF_ASSIGNABLE_ROLES.contains(requested)) {
                throw new InvalidRoleRequestException(requested);
            }
        }

        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.fullName(),
                request.phone());

        Set<String> rolesToGrant = new java.util.HashSet<>(request.roles());
        rolesToGrant.add("RENTER");
        for (String roleName : rolesToGrant) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new IllegalStateException(roleName + " role not seeded"));
            user.addRole(role);
        }

        userRepository.save(user);

        return issueTokenPair(user);
    }

    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public TokenPair login(LoginRequest request) {
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

        return issueTokenPair(user);
    }

    @Transactional
    public TokenPair refresh(String rawRefreshToken) {
        RefreshTokenService.RotationResult rotation = refreshTokenService.validateAndRotate(rawRefreshToken);
        User user = rotation.user();
        Set<String> roleNames = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        String accessToken = jwtService.issueAccessToken(user.getEmail(), roleNames);
        return new TokenPair(new AuthResponse(accessToken, user.getEmail(), roleNames), rotation.rawToken());
    }

    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    private TokenPair issueTokenPair(User user) {
        Set<String> roleNames = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        String accessToken = jwtService.issueAccessToken(user.getEmail(), roleNames);
        String rawRefreshToken = refreshTokenService.issue(user);
        return new TokenPair(new AuthResponse(accessToken, user.getEmail(), roleNames), rawRefreshToken);
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
