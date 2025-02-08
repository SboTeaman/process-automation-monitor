# Process Automation Monitor

![Java](https://img.shields.io/badge/Java_17-Spring_Boot_3-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Python](https://img.shields.io/badge/Python_3.11-FastAPI-009688?style=flat-square&logo=fastapi&logoColor=white)
![React](https://img.shields.io/badge/React_18-TypeScript-3178C6?style=flat-square&logo=react&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose_%2B_K8s-2496ED?style=flat-square&logo=docker&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?style=flat-square&logo=postgresql&logoColor=white)

A production-grade job scheduling and automation platform built with a microservices architecture. Schedule, execute, and monitor automated tasks — HTTP calls, CSV processing, data validation, and report generation — through a modern React dashboard.

> **Portfolio project** demonstrating full-stack microservices design across Java, Python, and TypeScript with real-world concerns: security, scheduling, analytics, and container orchestration.

---

## Screenshots

> _Add screenshots or a GIF of the dashboard here — e.g. job creation wizard, execution history, analytics charts._

---

## Architecture

```
┌────────────────────────────────────────────────────────────┐
│               React + TypeScript Frontend  :3000           │
│  Dashboard · Jobs · Analytics · Alerts · Execution History │
└──────────────────────────┬─────────────────────────────────┘
                           │ REST + JWT
              ┌────────────▼────────────┐
              │   Orchestrator Service  │  Java 17 · Spring Boot 3
              │        :8080            │  Quartz Scheduler · PostgreSQL
              └──────┬──────────┬───────┘
                     │          │
          ┌──────────▼──┐  ┌────▼───────────────┐
          │    Worker   │  │  Analytics Service │
          │   Service   │  │       :8001        │
          │    :8000    │  │  Python · FastAPI  │
          │   Python    │  │  ETL · MySQL       │
          │   FastAPI   │  └────────────────────┘
          │   MySQL     │
          └─────────────┘
```

**Orchestrator** owns business logic, scheduling, auth, and job state.
**Worker** executes jobs (HTTP, CSV, validation, reports) and stores results.
**Analytics** runs an hourly ETL pipeline to aggregate metrics and trends.

---

## Technical Highlights

**Multi-language microservices** — Chose the right tool per domain: Java/Spring Boot for the stateful orchestrator (mature ecosystem for scheduling and security), Python/FastAPI for execution workers (async I/O fits I/O-heavy jobs), React/TypeScript for a type-safe, component-driven UI.

**Quartz Scheduler** — Persistent cron-based job scheduling via Quartz. Jobs survive service restarts; state is stored in PostgreSQL, not in memory. Supports on-demand manual triggers alongside scheduled runs.

**JWT authentication with auto-refresh & revocation** — 15-minute access tokens paired with 7-day refresh tokens. Token revocation is persisted to PostgreSQL and survives restarts / multi-instance deployments. An Axios interceptor on the frontend silently refreshes expired tokens and retries the original request — users never see a session expiry pop-up.

**Role-based access control** — `ADMIN` and `OPERATOR` roles enforced at the API level. Operators can only manage their own jobs; admins get full access. Validated on every endpoint via Spring Security.

**Service-to-service authentication** — Worker and Analytics services require `X-Worker-Api-Key` and `X-Analytics-Api-Key` headers respectively, preventing unauthorized callers from triggering jobs or accessing metrics.

**Separated analytics pipeline** — Analytics runs an independent ETL hourly, reading from the orchestrator's PostgreSQL and writing aggregated stats into its own MySQL. This keeps analytical queries from affecting transactional performance.

**Container-first deployment** — A single `docker-compose up` spins up all 3 services, 3 databases, and health checks. Kubernetes manifests with HPA are included for production horizontal scaling.

**Stateless orchestrator** — Session state lives in JWTs, not server memory, making the orchestrator trivially horizontally scalable.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 18, TypeScript, Vite, Tailwind CSS, Zustand, Recharts |
| Orchestrator | Java 17, Spring Boot 3, Spring Security, Quartz, JPA, PostgreSQL |
| Worker | Python 3.11, FastAPI, SQLAlchemy, Alembic, MySQL |
| Analytics | Python 3.11, FastAPI, SQLAlchemy, Alembic, MySQL |
| Infrastructure | Docker Compose, Kubernetes (HPA), nginx |
| Auth | JWT (access + refresh tokens, DB-persisted revocation), BCrypt 12, API keys |
| Security | SSRF protection, path traversal protection, SQL injection mitigation, rate limiting |
| Docs | Swagger / OpenAPI (disabled by default, opt-in via `SWAGGER_ENABLED=true`) |

---

## Quick Start

**Prerequisites:** Docker and Docker Compose

```bash
# Clone and setup
git clone <repo-url>
cd process-automation-monitor

# Create .env with required secrets
cat > .env <<EOF
JWT_SECRET=$(openssl rand -base64 32)
WORKER_API_KEY=$(openssl rand -base64 32)
ANALYTICS_API_KEY=$(openssl rand -base64 32)
POSTGRES_PASSWORD=$(openssl rand -base64 16)
EOF

# Start everything
docker-compose up -d
```

⚠️ **Important:** Never commit `.env` to git. The above generates random secrets for local development. For production, use a secrets manager (AWS Secrets Manager, HashiCorp Vault, etc.).

| Service | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| Orchestrator API | http://localhost:8080/api/... |
| Orchestrator Swagger | http://localhost:8080/swagger-ui.html (if `SWAGGER_ENABLED=true`) |
| Worker health | http://localhost:8000/health |
| Analytics health | http://localhost:8001/health |

**First use:**
1. Go to http://localhost:3000/register and create an account
2. Log in and open **Jobs > Create Job**
3. Follow the 3-step wizard to schedule your first job
4. Watch execution results appear in **Execution History** and **Analytics**

---

## Job Types

| Type | What it does |
|------|-------------|
| `HTTP_CALL` | Makes an HTTP request (GET/POST/PUT/DELETE) to any URL with custom headers and body |
| `CSV_PROCESS` | Reads, transforms, and writes CSV files |
| `DATA_VALIDATE` | Validates data against configurable rules (format, range, required fields) |
| `REPORT_GENERATE` | Generates reports from data and distributes them via email |

---

## API Overview

```
POST   /auth/login                 — Login, returns JWT pair
POST   /auth/register              — Register new account

GET    /jobs                       — List jobs (paginated, filterable)
POST   /jobs                       — Create job
PUT    /jobs/{id}                  — Update job
POST   /jobs/{id}/trigger          — Run job immediately
PATCH  /jobs/{id}/toggle           — Enable / disable job

GET    /executions                 — Execution history (paginated)
GET    /stats/summary              — Dashboard metrics (24h window)
GET    /stats/top-failing          — Jobs with most failures
GET    /alerts                     — Active alerts
```

Full reference available at `/swagger-ui.html` when the orchestrator is running (enable with `SWAGGER_ENABLED=true`).

**Note:** Worker and Analytics services require API key authentication:
- Worker: `X-Worker-Api-Key: <WORKER_API_KEY>`
- Analytics: `X-Analytics-Api-Key: <ANALYTICS_API_KEY>`

---

## Security

**SSRF Protection** — HTTP executor blocks requests to private IP ranges (RFC 1918, link-local, cloud metadata endpoints). DNS-rebinding attacks are mitigated by checking the resolved IP *at connection time*, not before DNS resolution.

**Path Traversal Protection** — All file operations (CSV read/write, report generation) are sandboxed to `DATA_DIR`. Symbolic links and `..` traversal attempts are rejected.

**SQL Injection Mitigation** — Report queries are validated against a keyword blocklist and a whitelist of allowed tables. Only read-only `SELECT` queries on `job_results` table are permitted.

**CORS & Headers** — Allowed origins are configurable. Allowed headers are restricted to `Authorization`, `Content-Type`, `X-Correlation-Id`, and `X-Requested-With`. Swagger / OpenAPI are disabled by default.

**Rate Limiting** — `/auth/login` and `/auth/register` are limited to 10 requests per minute per IP. X-Forwarded-For is only trusted when the direct TCP connection arrives from a configured trusted proxy.

**API Key Authentication** — Worker and Analytics services require API key headers to prevent unauthorized inter-service calls.

**JWT Revocation** — Token revocation is persisted to PostgreSQL. Survives restarts and multi-instance deployments.

### Database Layer

**SSL/TLS Connections** — PostgreSQL and MySQL connections use `sslmode=require` (configurable). Self-signed certificates provided for development; use CA-signed certs in production. Generate with: `scripts/generate-db-certs.sh`

**Specialized Database Roles** — Each service has a dedicated database user with principle-of-least-privilege:
  - **Orchestrator**: full write access to all tables
  - **Analytics**: read-only access (SELECT only)

**Row-Level Security (RLS)** — PostgreSQL RLS policies enforce multi-tenancy:
  - Operators see only jobs they created
  - Admins see all jobs
  - Analytics cannot modify any data
  - Users cannot see other users' details

**Encrypted Secrets** — Kubernetes Secrets are recommended to be encrypted using **SealedSecrets** (controller-based encryption) or **External Secrets** (integration with AWS Secrets Manager, Azure KeyVault, HashiCorp Vault). Documentation: `k8s/sealed-secrets-setup.md`

**Automated Backups** — Daily CronJobs backup both PostgreSQL and MySQL databases. Backups are retained for 30 days locally and optionally synced to S3. Kubernetes manifests: `k8s/backups-cronjob.yaml`

---

## Project Structure

```
.
├── frontend/                  # React application (TypeScript + Vite)
│   └── src/
│       ├── pages/             # Dashboard, Jobs, Analytics, Alerts, History
│       ├── components/        # Shared UI components
│       ├── api/               # Axios client with JWT interceptor
│       └── store/             # Zustand state management
│
└── process-automation-monitor/
    ├── orchestrator-service/  # Java Spring Boot — core logic, auth, scheduling
    ├── worker-service/        # Python FastAPI — job execution
    ├── analytics-service/     # Python FastAPI — ETL and reporting
    ├── k8s/                   # Kubernetes manifests (deployments, HPA, secrets)
    └── docker-compose.yml     # Full local stack
```

---

## Development Setup (without Docker)

**Backend:**
```bash
cd process-automation-monitor/orchestrator-service
./mvnw spring-boot:run
```

**Worker / Analytics:**
```bash
cd process-automation-monitor/worker-service
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```

**Frontend:**
```bash
cd frontend
npm install
npm run dev        # http://localhost:3000
```

---

## License

MIT
