package com.sentinelx.sentinelx.service;

import com.sentinelx.sentinelx.dto.IngestDtos;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class SimulatorService {

    private static final String[] ATTACKER_IPS = {
            "45.33.32.156", "185.220.101.34", "91.240.118.172", "103.208.220.11"};
    private static final String[] TARGET_USERS = {"alice", "bob", "admin", "svc_backup", "root"};

    private final LogIngestionService ingestion;

    public SimulatorService(LogIngestionService ingestion) {
        this.ingestion = ingestion;
    }

    public IngestDtos.SimulatorResult run(String scenario) {
        return switch (scenario == null ? "" : scenario.toLowerCase()) {
            case "normal-login" -> normalLogin();
            case "brute-force" -> bruteForce();
            case "sql-injection" -> sqlInjection();
            case "xss" -> xssAttempt();
            case "ddos" -> ddosBurst();
            case "port-scan" -> portScan();
            case "insider-anomaly" -> insiderAnomaly();
            default -> throw new IllegalArgumentException("Unknown scenario: " + scenario);
        };
    }

    public List<String> scenarioNames() {
        return List.of("normal-login", "brute-force", "sql-injection", "xss",
                "ddos", "port-scan", "insider-anomaly");
    }

    private IngestDtos.SimulatorResult normalLogin() {
        List<IngestDtos.ProcessingOutcome> outcomes = new ArrayList<>();
        outcomes.add(ingestion.ingest(event(
                attackerIp(), "alice", "/api/login", "POST", 200,
                0, 1200, "LOGIN_SUCCESS", null)));
        return aggregate("normal-login", outcomes);
    }

    private IngestDtos.SimulatorResult bruteForce() {
        String ip = attackerIp();
        List<IngestDtos.ProcessingOutcome> outcomes = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            outcomes.add(ingestion.ingest(event(
                    ip, TARGET_USERS[i % TARGET_USERS.length], "/api/login", "POST",
                    401, i + 1, 180, "LOGIN_FAILED", null)));
        }
        return aggregate("brute-force", outcomes);
    }

    private IngestDtos.SimulatorResult sqlInjection() {
        String payload = "?id=105%20OR%201=1 UNION SELECT username,password FROM users--";
        List<IngestDtos.ProcessingOutcome> outcomes = new ArrayList<>();
        outcomes.add(ingestion.ingest(event(
                attackerIp(), "-", "/products" + payload.replace(' ', '+'), "GET",
                500, 0, 640, "HTTP_ERROR", "GET /products?id=105 OR 1=1 UNION SELECT username,password FROM users--")));
        outcomes.add(ingestion.ingest(event(
                attackerIp(), "-", "/search?q=%27%20OR%20%271%27%3D%271", "GET",
                200, 0, 410, "HTTP_REQUEST", "GET /search?q=' OR '1'='1")));
        return aggregate("sql-injection", outcomes);
    }

    private IngestDtos.SimulatorResult xssAttempt() {
        String script = "<script>alert(document.cookie)</script>";
        List<IngestDtos.ProcessingOutcome> outcomes = new ArrayList<>();
        outcomes.add(ingestion.ingest(event(
                attackerIp(), "guest", "/comments", "POST", 200, 0, 520,
                "HTTP_REQUEST", "POST /comments body={\"text\":\"" + script + "\"}")));
        return aggregate("xss", outcomes);
    }

    private IngestDtos.SimulatorResult ddosBurst() {
        String ip = attackerIp();
        List<IngestDtos.ProcessingOutcome> outcomes = new ArrayList<>();
        for (int i = 0; i < 110; i++) {
            outcomes.add(ingestion.ingest(event(
                    ip, "-", "/api/catalog?page=" + (i % 5), "GET", 200, 0,
                    90, "HTTP_REQUEST", null)));
        }
        return aggregate("ddos", outcomes);
    }

    private IngestDtos.SimulatorResult portScan() {
        String ip = attackerIp();
        List<IngestDtos.ProcessingOutcome> outcomes = new ArrayList<>();
        int basePort = 1000 + ThreadLocalRandom.current().nextInt(500);
        for (int p = 0; p < 18; p++) {
            outcomes.add(ingestion.ingest(event(
                    ip, "-", null, "TCP", 0, 0, 40,
                    "CONNECTION_ATTEMPT", null, basePort + p * 7)));
        }
        return aggregate("port-scan", outcomes);
    }

    private IngestDtos.SimulatorResult insiderAnomaly() {
        Instant now = Instant.now();
        List<IngestDtos.ProcessingOutcome> outcomes = new ArrayList<>();
        outcomes.add(ingestion.ingest(new IngestDtos.IngestRequest(
                "EVT-INS-" + System.nanoTime(), now.minusSeconds(3600 * 24),
                "10.0.2.15", "10.0.5.30", "svc_backup", "internal-app", "/files/export/finances_q2.zip",
                "GET", 200, "HTTPS", 443, 850_000_000L, 0, "LOGIN_SUCCESS",
                "Legitimate credentials used at unusual hour with bulk export")));
        outcomes.add(ingestion.ingest(event(
                "10.0.2.15", "svc_backup", "/files/download/all_projects.tar.gz", "GET",
                200, 0, 1_200_000_000L, "FILE_DOWNLOAD", null)));
        return aggregate("insider-anomaly", outcomes);
    }

    private IngestDtos.IngestRequest event(String sourceIp, String username, String endpoint,
                                           String method, int status, int failedAttempts,
                                           long bytes, String eventType, String rawMessage) {
        return event(sourceIp, username, endpoint, method, status, failedAttempts, bytes, eventType, rawMessage, 443);
    }

    private IngestDtos.IngestRequest event(String sourceIp, String username, String endpoint,
                                           String method, int status, int failedAttempts,
                                           long bytes, String eventType, String rawMessage, int port) {
        return new IngestDtos.IngestRequest(
                "EVT-SIM-" + ThreadLocalRandom.current().nextInt(1_000_000),
                Instant.now(),
                sourceIp, "10.0.0.10", username, "web-app", endpoint,
                method, status, method.startsWith("T") ? "TCP" : "HTTPS", port,
                bytes, failedAttempts, eventType, rawMessage);
    }

    private static String attackerIp() {
        return ATTACKER_IPS[ThreadLocalRandom.current().nextInt(ATTACKER_IPS.length)];
    }

    private IngestDtos.SimulatorResult aggregate(String scenario, List<IngestDtos.ProcessingOutcome> outcomes) {
        List<IngestDtos.AlertSummary> alerts = new ArrayList<>();
        List<java.util.UUID> incidentIds = new ArrayList<>();
        int aiFlagged = 0;
        for (var o : outcomes) {
            alerts.addAll(o.alerts());
            for (java.util.UUID id : o.incidentIds()) {
                if (!incidentIds.contains(id)) {
                    incidentIds.add(id);
                }
            }
            if (Boolean.TRUE.equals(o.aiAnomaly())) {
                aiFlagged++;
            }
        }
        return new IngestDtos.SimulatorResult(scenario, outcomes.size(), alerts, incidentIds, aiFlagged);
    }
}
