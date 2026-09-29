# SentinelX — Security Operations & Threat Detection Platform

SentinelX is an end-to-end SIEM-style platform that ingests security events, detects attacks with a
hybrid **rules + machine-learning** engine, scores risk, raises alerts, correlates them into
investigable incidents, and presents everything through a real-time SOC dashboard.

## Architecture

```
┌─────────────┐   events    ┌──────────────────┐   features   ┌───────────────┐
│  Simulator  │ ──────────▶ │  Backend (Java)  │ ───────────▶ │ ML Service    │
│  (Python)   │  /api/logs  │  Spring Boot     │  HTTP :8000  │ FastAPI +     │
└─────────────┘             │                  │ ◀─────────── │ NumPy         │
                            │  • Rule Engine   │  anomaly     │ Autoencoder   │
┌─────────────┐   REST/JWT  │  • Risk Scoring  │  score       └───────────────┘
│  Frontend   │ ◀─────────  │  • Incidents     │
│  React+Vite │             │  • Threat Intel  │      ┌───────────────┐
│  :5173      │             │  • Audit / RBAC  │ ───▶ │ PostgreSQL    │
└─────────────┘             └──────────────────┘ JPA  │ :5432         │
                                                     └───────────────┘
```

| Component | Stack | Port |
|---|---|---|
| Frontend | React 18, Vite, React Router | 5173 |
| Backend API | Java 17, Spring Boot 3.4, Spring Security (JWT), JPA | 8080 |
| ML Service | Python 3.12, FastAPI, NumPy autoencoder | 8000 |
| Database | PostgreSQL 16 | 5432 |

## Quick Start (Docker)

```bash
cp .env.example .env          # edit secrets if desired
docker compose up --build -d
```

Then:
- Dashboard: http://localhost:5173 (`npm install && npm run dev` inside `frontend/`)
- API: http://localhost:8080
- ML health: http://localhost:8000/health

> The ML model ships pre-trained (`ml-service/models/autoencoder.npz`). To retrain locally:
> `pip install -r ml-service/requirements.txt && python ml-service/train.py`

### Quick Start (Windows, no Docker / no PostgreSQL)

If Docker and PostgreSQL are not installed, run the backend against the bundled
embedded **H2 in-memory database** (`application-h2.yml`) instead of Postgres.
One command builds the backend (auto-downloads Maven) and starts everything:

```powershell
powershell -ExecutionPolicy Bypass -File run.ps1          # start all services
powershell -ExecutionPolicy Bypass -File run.ps1 -Build   # force a rebuild first
powershell -ExecutionPolicy Bypass -File run.ps1 -Stop    # stop all services
```

Prerequisites already required on the box: **Java 17+**, **Node** (`npm.cmd`),
**Python** (with `fastapi`, `uvicorn`, `numpy`, `requests` for the ml-service and
simulator). With the H2 profile **no database server is needed** — data is
in-memory and resets when the backend restarts.

Equivalent manual steps (H2 path):

```bash
# 1. ML service
cd ml-service && pip install -r requirements.txt && uvicorn main:app --port 8000

# 2. Backend (embedded H2, no Postgres)
cd backend && mvn -DskipTests package
java -jar target/sentinelx-backend-1.0.0.jar --spring.profiles.active=h2

# 3. Frontend
cd frontend && npm install && npm run dev

# 4. Traffic
cd simulator && pip install -r requirements.txt && python simulate.py --all
```

## Quick Start (Manual Dev)

1. **Database** — `docker compose up -d db`
2. **ML service** — `cd ml-service && pip install -r requirements.txt && python train.py && uvicorn main:app --port 8000`
3. **Backend** — `cd backend && mvn spring-boot:run`
4. **Frontend** — `cd frontend && npm install && npm run dev`
5. **Generate traffic** — `cd simulator && pip install -r requirements.txt && python simulate.py --all`

### Seeded accounts

| Role | Username | Password |
|---|---|---|
| ADMIN | `admin` | `Admin@123` |
| ANALYST | `analyst` | `Analyst@123` |

## Detection Pipeline

Every event posted to `/api/logs` flows through:

1. **Persistence & enrichment** — normalized and stored with source metadata.
2. **Rule Engine** (deterministic):
   - `BRUTE_FORCE` — failed-login threshold within sliding window
   - `DDOS_RATE` — request volume threshold within window
   - `PORT_SCAN` — distinct TCP ports probed in window
   - `SQL_INJECTION` — payload pattern match on endpoint/raw message
   - `XSS` — script injection patterns
3. **ML Anomaly Detection** — the backend extracts numeric features (request rate, bytes,
   failure counts, hour-of-day, …) and calls the autoencoder; reconstruction error above the
   trained threshold flags anomalies even when no rule matches.
4. **Risk Scoring** — weighted blend of rule confidence, AI anomaly score, repeat-offender
   history, and threat-intel reputation → 0–100 risk score.
5. **Alerting & Correlation** — high-risk events become alerts; alerts ≥ `incident-threshold`
   are deduplicated into incidents per attacker/target within a time window.
6. **Threat Intel** — AbuseIPDB lookups (API key optional; heuristic fallback offline) cached 24 h.
7. **Audit** — logins, triage actions, admin changes recorded for compliance.

## Simulator Scenarios

Run via UI (**Simulator** page), built-in API (`POST /api/simulator/{scenario}`), or standalone CLI:

| Scenario | Expected outcome |
|---|---|
| `normal-login` | Benign baseline — no detections |
| `brute-force` | Rule alert (HIGH) |
| `sql-injection` | Rule alert via payload matching |
| `xss` | Rule alert on `<script>` payload |
| `ddos` | Rate-rule alert from volume burst |
| `port-scan` | Alert on distinct-port probing |
| `insider-anomaly` | **ML-only** detection — valid credentials, unusual behavior |

CLI examples:

```bash
python simulate.py --list
python simulate.py --scenario brute-force
python simulate.py --scenario ddos --count 3 --interval 2
python simulate.py --all --interval 5 --normal-ratio 0.7   # endless mixed traffic
```

## API Overview (JWT bearer required except `/api/auth/**`)

| Area | Endpoints |
|---|---|
| Auth | `POST /api/auth/register` · `POST /api/auth/login` · `GET /api/auth/me` |
| Dashboard | `GET /api/dashboard/summary` · `/trends` |
| Logs | `POST /api/logs` · `POST /api/logs/batch` · `GET /api/logs?q&eventType&page&size` |
| Alerts | `GET /api/alerts?severity&status&q` · `PUT /api/alerts/{id}/status` |
| Incidents | `GET /api/incidents?status&q` · `GET/PUT /api/incidents/{id}` |
| Rules | `GET /api/rules` · `PUT /api/rules/{id}` *(admin)* |
| Threat Intel | `GET /api/threat-intel/ip/{ip}` · `/recent` |
| Simulator | `POST /api/simulator/{scenario}` · `GET /api/simulator/scenarios` |
| Reports | `GET /api/reports/summary?days` · `/summary/download` |
| System | `GET /api/system/info` · `GET /api/system/ml-health` |
| Users *(admin)* | `GET/POST /api/users` · `PUT /api/users/{id}` |
| Audit *(admin)* | `GET /api/audit?page&size` |

## Configuration

All tunables live in `.env` (see `.env.example`) and `backend/src/main/resources/application.yml`
(JWT secret/expiry, CORS origins, risk weights, incident threshold & dedupe window, rule defaults).

## Project Layout

```
backend/     Spring Boot API, rule engine, risk scoring, incidents, RBAC
ml-service/  FastAPI inference + NumPy autoencoder training (train.py)
frontend/    React SOC dashboard (Vite)
simulator/   Standalone attack-traffic generator (Python CLI)
docs/        This documentation
docker-compose.yml  Postgres + ML + backend orchestration
```

## Deploying to AWS (single EC2 + Docker)

`docker-compose.yml` now includes a `frontend` service (nginx) that serves the
production React build and proxies `/api` to the backend, so one EC2 instance can
host the entire platform on **port 80**. Example: `http://<your-ec2-public-ip>`.

Full, beginner-friendly walkthrough (account creation → instance launch → deploy):
see **[docs/AWS_DEPLOY.md](AWS_DEPLOY.md)**.

Quick inside-EC2 command after uploading and extracting the source:

```bash
sudo bash deploy/install.sh          # installs Docker, builds, and starts everything
```

## Testing

```bash
cd backend && mvn test          # rule engine, risk scoring, incident lifecycle tests
cd frontend && npm run build    # production build check
```
