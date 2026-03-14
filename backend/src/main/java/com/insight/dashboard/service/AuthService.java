package com.insight.dashboard.service;

import com.insight.dashboard.dto.AuthResponse;
import com.insight.dashboard.dto.LoginRequest;
import com.insight.dashboard.dto.RegisterRequest;
import com.insight.dashboard.dto.UserResponse;
import com.insight.dashboard.entity.AppUser;
import com.insight.dashboard.exception.BadRequestException;
import com.insight.dashboard.repository.AppUserRepository;
import com.insight.dashboard.security.AuthPrincipal;
import com.insight.dashboard.security.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(AppUserRepository appUserRepository,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (request.getFullName() == null || request.getFullName().isBlank()) {
            throw new BadRequestException("fullName is required");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new BadRequestException("email is required");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new BadRequestException("password must be at least 6 characters");
        }

        String email = request.getEmail().trim().toLowerCase();
        appUserRepository.findByEmailIgnoreCase(email).ifPresent(existing -> {
            throw new BadRequestException("An account already exists for this email.");
        });

        AppUser user = new AppUser();
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        appUserRepository.save(user);
        return authenticate(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank() || request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BadRequestException("email and password are required");
        }

        AppUser user = appUserRepository.findByEmailIgnoreCase(request.getEmail().trim().toLowerCase())
            .orElseThrow(() -> new BadRequestException("Invalid email or password."));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password.");
        }
        return authenticate(user);
    }

    @Transactional(readOnly = true)
    public UserResponse me(AuthPrincipal principal) {
        AppUser user = appUserRepository.findById(principal.userId())
            .orElseThrow(() -> new BadRequestException("User not found."));
        return toUserResponse(user);
    }

    private AuthResponse authenticate(AppUser user) {
        return new AuthResponse(tokenService.generateToken(user.getId(), user.getEmail()), toUserResponse(user));
    }

    private UserResponse toUserResponse(AppUser user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail());
    }
}
