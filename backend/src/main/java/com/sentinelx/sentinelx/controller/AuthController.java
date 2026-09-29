package com.sentinelx.sentinelx.controller;

import com.sentinelx.sentinelx.dto.AuthDtos;
import com.sentinelx.sentinelx.entity.SecurityEvent;
import com.sentinelx.sentinelx.repository.SecurityEventRepository;
import com.sentinelx.sentinelx.service.AuditService;
import com.sentinelx.sentinelx.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuditService auditService;
    private final SecurityEventRepository securityEventRepository;

    public AuthController(AuthService authService,
                          AuditService auditService,
                          SecurityEventRepository securityEventRepository) {
        this.authService = authService;
        this.auditService = auditService;
        this.securityEventRepository = securityEventRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthDtos.AuthResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest req,
                                                          HttpServletRequest http) {
        AuthDtos.AuthResponse resp = authService.register(req);
        auditService.log(resp.user().username(), "USER_REGISTERED",
                "role=" + resp.user().role(), http);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDtos.AuthResponse> login(@Valid @RequestBody AuthDtos.LoginRequest req,
                                                       HttpServletRequest http) {
        try {
            AuthDtos.AuthResponse resp = authService.login(req);
            auditService.log(req.username(), "LOGIN_SUCCESS", "jwt issued", http);
            recordLoginEvent(req.username(), "LOGIN_SUCCESS", 200, AuditService.clientIp(http));
            return ResponseEntity.ok(resp);
        } catch (Exception ex) {
            auditService.log(req.username(), "LOGIN_FAILED", "invalid credentials", http);
            recordLoginEvent(req.username(), "LOGIN_FAILED", 401, AuditService.clientIp(http));
            throw ex;
        }
    }

    private void recordLoginEvent(String username, String eventType, int status, String ip) {
        try {
            SecurityEvent e = new SecurityEvent();
            e.setEventId("EVT-UI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            e.setTimestamp(Instant.now());
            e.setSourceIp(ip);
            e.setUsername(username);
            e.setSource("auth-ui");
            e.setEndpoint("/api/auth/login");
            e.setHttpMethod("POST");
            e.setStatus(status);
            e.setProtocol("HTTPS");
            e.setPort(443);
            e.setBytes(0L);
            e.setFailedAttempts(0);
            e.setEventType(eventType);
            e.setProcessed(true);
            securityEventRepository.save(e);
        } catch (Exception ignored) {
            // logging an authentication event must never break the auth flow
        }
    }

    @GetMapping("/me")
    public ResponseEntity<AuthDtos.UserDto> me(Authentication authentication) {
        return ResponseEntity.ok(authService.me(authentication.getName()));
    }
}
