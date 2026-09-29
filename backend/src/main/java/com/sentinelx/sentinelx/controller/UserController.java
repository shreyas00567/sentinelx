package com.sentinelx.sentinelx.controller;

import com.sentinelx.sentinelx.dto.ConfigDtos;
import com.sentinelx.sentinelx.entity.User;
import com.sentinelx.sentinelx.entity.UserRole;
import com.sentinelx.sentinelx.exception.ApiException;
import com.sentinelx.sentinelx.repository.UserRepository;
import com.sentinelx.sentinelx.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                          AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<List<ConfigDtos.UserDto>> list() {
        return ResponseEntity.ok(userRepository.findAll().stream().map(UserController::toDto).toList());
    }

    @PostMapping
    public ResponseEntity<ConfigDtos.UserDto> create(@Valid @RequestBody ConfigDtos.CreateUserRequest req,
                                                     HttpServletRequest http) {
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
        user.setRole(parseRole(req.role()));
        user.setEnabled(true);
        user.setCreatedAt(Instant.now());
        user = userRepository.save(user);
        auditService.log(req.username(), "USER_CREATED", "role=" + user.getRole(), http);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConfigDtos.UserDto> update(@PathVariable Long id,
                                                     @Valid @RequestBody ConfigDtos.UpdateUserRequest req,
                                                     HttpServletRequest http) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        if (req.enabled() != null) {
            user.setEnabled(req.enabled());
        }
        if (req.password() != null && !req.password().isBlank()) {
            if (req.password().length() < 8) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters");
            }
            user.setPasswordHash(passwordEncoder.encode(req.password()));
        }
        if (req.role() != null && !req.role().isBlank()) {
            user.setRole(parseRole(req.role()));
        }
        if (req.fullName() != null) {
            user.setFullName(req.fullName());
        }
        user = userRepository.save(user);
        auditService.log(user.getUsername(), "USER_UPDATED",
                "enabled=" + user.isEnabled() + " role=" + user.getRole(), http);
        return ResponseEntity.ok(toDto(user));
    }

    private static UserRole parseRole(String role) {
        try {
            return UserRole.valueOf(role == null ? "ANALYST" : role.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid role: " + role);
        }
    }

    private static ConfigDtos.UserDto toDto(User u) {
        return new ConfigDtos.UserDto(u.getId(), u.getUsername(), u.getEmail(), u.getFullName(),
                u.getRole().name(), u.isEnabled(), u.getCreatedAt());
    }
}
