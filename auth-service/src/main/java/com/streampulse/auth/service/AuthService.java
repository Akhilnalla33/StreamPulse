package com.streampulse.auth.service;

import com.streampulse.auth.dto.LoginRequest;
import com.streampulse.auth.dto.RefreshRequest;
import com.streampulse.auth.dto.RegisterTenantRequest;
import com.streampulse.auth.dto.TokenResponse;
import com.streampulse.auth.entity.RefreshToken;
import com.streampulse.auth.entity.Role;
import com.streampulse.auth.entity.Tenant;
import com.streampulse.auth.entity.User;
import com.streampulse.auth.exception.AuthException;
import com.streampulse.auth.repository.RefreshTokenRepository;
import com.streampulse.auth.repository.TenantRepository;
import com.streampulse.auth.repository.UserRepository;
import com.streampulse.auth.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            TenantRepository tenantRepository,
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public TokenResponse registerTenant(RegisterTenantRequest request) {
        if (tenantRepository.existsBySlug(request.tenantSlug())) {
            throw AuthException.tenantSlugTaken(request.tenantSlug());
        }
        Tenant tenant = new Tenant(UUID.randomUUID(), request.tenantName(), request.tenantSlug(), Instant.now());
        tenantRepository.save(tenant);

        User admin = new User(
                UUID.randomUUID(),
                tenant.getId(),
                request.adminEmail(),
                passwordEncoder.encode(request.adminPassword()),
                Set.of(Role.ADMIN),
                Instant.now());
        userRepository.save(admin);

        return issueTokenPair(admin);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        Tenant tenant = tenantRepository.findBySlug(request.tenantSlug())
                .orElseThrow(AuthException::invalidCredentials);
        User user = userRepository.findByTenantIdAndEmail(tenant.getId(), request.email())
                .orElseThrow(AuthException::invalidCredentials);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw AuthException.invalidCredentials();
        }
        return issueTokenPair(user);
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        Claims claims;
        try {
            claims = jwtService.parseAndValidate(request.refreshToken());
        } catch (JwtException e) {
            throw AuthException.invalidRefreshToken();
        }
        if (!jwtService.isRefreshToken(claims)) {
            throw AuthException.invalidRefreshToken();
        }

        String tokenHash = hash(request.refreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(AuthException::invalidRefreshToken);
        if (!stored.isValid(Instant.now())) {
            throw AuthException.invalidRefreshToken();
        }
        stored.revoke();
        refreshTokenRepository.save(stored);

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(AuthException::invalidRefreshToken);
        return issueTokenPair(user);
    }

    private TokenResponse issueTokenPair(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getTenantId(), user.getRoles());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getTenantId(), user.getRoles());

        RefreshToken record = new RefreshToken(
                UUID.randomUUID(),
                user.getId(),
                hash(refreshToken),
                Instant.now().plus(jwtService.refreshTokenTtlSeconds(), ChronoUnit.SECONDS),
                Instant.now());
        refreshTokenRepository.save(record);

        return TokenResponse.bearer(accessToken, refreshToken, jwtService.accessTokenTtlSeconds());
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
