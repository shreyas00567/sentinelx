package com.sentinelx.sentinelx.service;

import com.sentinelx.sentinelx.dto.AuthDtos;
import com.sentinelx.sentinelx.entity.User;
import com.sentinelx.sentinelx.entity.UserRole;
import com.sentinelx.sentinelx.exception.ApiException;
import com.sentinelx.sentinelx.repository.UserRepository;
import com.sentinelx.sentinelx.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new ApiException(HttpStatus.CONFLICT, "Username already taken");
        }
        if (userRepository.existsByEmail(req.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }
        User user = new User();
        user.setUsername(req.username().trim());
        user.setEmail(req.email().trim());
        user.setFullName(req.fullName() == null ? "" : req.fullName().trim());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(userRepository.count() == 0 ? UserRole.ADMIN : UserRole.ANALYST);
        user.setEnabled(true);
        user.setCreatedAt(Instant.now());
        user = userRepository.save(user);
        return new AuthDtos.AuthResponse(jwtService.generateToken(user), toDto(user));
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest req) {
        User user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!user.isEnabled() || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return new AuthDtos.AuthResponse(jwtService.generateToken(user), toDto(user));
    }

    public AuthDtos.UserDto me(String username) {
        return userRepository.findByUsername(username)
                .map(this::toDto)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public AuthDtos.UserDto toDto(User u) {
        return new AuthDtos.UserDto(u.getId(), u.getUsername(), u.getEmail(),
                u.getFullName(), u.getRole().name(), u.isEnabled(), u.getCreatedAt());
    }
}
