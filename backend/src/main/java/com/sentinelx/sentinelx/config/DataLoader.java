package com.sentinelx.sentinelx.config;

import com.sentinelx.sentinelx.entity.DetectionRuleEntity;
import com.sentinelx.sentinelx.entity.Severity;
import com.sentinelx.sentinelx.entity.User;
import com.sentinelx.sentinelx.entity.UserRole;
import com.sentinelx.sentinelx.repository.DetectionRuleRepository;
import com.sentinelx.sentinelx.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

@Configuration
public class DataLoader {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    @Bean
    CommandLineRunner seed(@Value("${sentinelx.seed.admin-username}") String adminUser,
                           @Value("${sentinelx.seed.admin-password}") String adminPass,
                           @Value("${sentinelx.seed.analyst-password}") String analystPass,
                           UserRepository userRepository,
                           DetectionRuleRepository ruleRepository,
                           PasswordEncoder encoder) {
        return args -> {
            if (!userRepository.existsByUsername(adminUser)) {
                User admin = new User();
                admin.setUsername(adminUser);
                admin.setEmail("admin@sentinelx.local");
                admin.setFullName("System Administrator");
                admin.setPasswordHash(encoder.encode(adminPass));
                admin.setRole(UserRole.ADMIN);
                admin.setEnabled(true);
                admin.setCreatedAt(Instant.now());
                userRepository.save(admin);
                log.info("Seeded ADMIN user '{}'", adminUser);

                User analyst = new User();
                analyst.setUsername("analyst");
                analyst.setEmail("analyst@sentinelx.local");
                analyst.setFullName("Security Analyst");
                analyst.setPasswordHash(encoder.encode(analystPass));
                analyst.setRole(UserRole.ANALYST);
                analyst.setEnabled(true);
                analyst.setCreatedAt(Instant.now());
                userRepository.save(analyst);
                log.info("Seeded ANALYST user 'analyst'");
            }

            if (ruleRepository.count() == 0) {
                seedRule(ruleRepository, "SQL_INJECTION", "SQL Injection",
                        "Detects SQL injection signatures in request paths and payloads", 1, 1, Severity.HIGH);
                seedRule(ruleRepository, "XSS", "Cross-Site Scripting",
                        "Detects script injection and event-handler payloads", 1, 1, Severity.HIGH);
                seedRule(ruleRepository, "BRUTE_FORCE", "Brute Force Login",
                        "Failed logins from one source within the time window", 5, 300, Severity.HIGH);
                seedRule(ruleRepository, "DDOS", "High-Rate DoS",
                        "Request rate per source exceeding the threshold per minute", 100, 60, Severity.CRITICAL);
                seedRule(ruleRepository, "PORT_SCAN", "Port Scan / Recon",
                        "Distinct destination ports touched by one source in the window", 15, 600, Severity.MEDIUM);
                log.info("Seeded default detection rules");
            }
        };
    }

    private void seedRule(DetectionRuleRepository repo, String key, String name, String desc,
                          int threshold, int windowSeconds, Severity severity) {
        DetectionRuleEntity r = new DetectionRuleEntity();
        r.setRuleKey(key);
        r.setName(name);
        r.setDescription(desc);
        r.setEnabled(true);
        r.setThreshold(threshold);
        r.setWindowSeconds(windowSeconds);
        r.setSeverity(severity);
        repo.save(r);
    }
}
