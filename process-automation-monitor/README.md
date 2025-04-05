# Process Automation Monitor

A system for defining, scheduling, and monitoring automated process tasks.
Inspired by the work of **Craftware** — a UiPath Platinum Partner specialising in
Robotic Process Automation (RPA) and enterprise automation for clients such as
Allegro, Orange, and Generali.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Docker Compose / K8s                         │
│                                                                     │
│  ┌───────────────────────┐        ┌────────────────────────┐        │
│  │   orchestrator-service│        │    worker-service      │        │
│  │   Java 17             │──REST──►   Python 3.11          │        │
│  │   Spring Boot         │◄──────│   FastAPI               │        │
│  │   PostgreSQL          │        │   MySQL                │        │
│  └──────────┬────────────┘        └────────────────────────┘        │
│             │                                                       │
│             │ (ETL / hourly CronJob)                                │
│             ▼                                                       │
│  ┌───────────────────────┐                                          │
│  │   analytics-service   │                                          │
│  │   Python 3.11         │                                          │
│  │   FastAPI             │                                          │
│  │   MySQL               │                                          │
│  └──────────┬────────────┘                                          │
│             │                                                       │
│     ┌───────▼─────────────────────────────┐                         │
│     │        API Gateway / nginx          │                         │
│     └─────────────────────────────────────┘                         │
└─────────────────────────────────────────────────────────────────────┘
```

### Data Flow

```
User defines a job
      ↓
orchestrator-service (Java) — stores, schedules, manages jobs
      ↓
worker-service (Python) — executes the job (HTTP call, CSV, validation)
      ↓
Result stored in PostgreSQL + execution log recorded
      ↓
ETL CronJob (K8s, every 1h) — aggregates data into MySQL (analytics)
      ↓
analytics-service — statistics, reports, alerts
```

---

## Technology Stack

| Component           | Technology                                   |
|---------------------|----------------------------------------------|
| orchestrator-service | Java 17, Spring Boot 3, Maven, PostgreSQL   |
| worker-service       | Python 3.11, FastAPI, SQLAlchemy, MySQL     |
| analytics-service    | Python 3.11, FastAPI, SQLAlchemy, MySQL     |
| Database migrations  | Flyway (Java), Alembic (Python)             |
| Containerisation     | Docker, Docker Compose                      |
| Orchestration        | Kubernetes (Deployment, CronJob, HPA, PVC)  |
| API documentation    | Swagger/OpenAPI, FastAPI autodocs           |

---

## Prerequisites

- Java 17+
- Maven 3.8+
- Python 3.11+
- Docker and Docker Compose v2
- kubectl (for Kubernetes deployment)

---

## Quick Start — Docker Compose

```bash
# 1. Copy and configure environment variables
cp .env.example .env
# Edit .env — set secure passwords and your JWT_SECRET

# 2. Start all services
docker-compose up -d

# 3. Check service health
docker-compose ps
```

Services will be available at:

| Service              | URL                                      |
|----------------------|------------------------------------------|
| orchestrator-service | http://localhost:8080                    |
| worker-service       | http://localhost:8000                    |
| analytics-service    | http://localhost:8001                    |

---

## API Documentation

| Service              | URL                                              |
|----------------------|--------------------------------------------------|
| orchestrator-service | http://localhost:8080/swagger-ui.html            |
| orchestrator-service | http://localhost:8080/v3/api-docs                |
| worker-service       | http://localhost:8000/docs                       |
| analytics-service    | http://localhost:8001/docs                       |

---

## Key Endpoints

### orchestrator-service (port 8080)

```
POST   /auth/register               Register a new account
POST   /auth/login                  Login — returns access + refresh tokens
POST   /auth/refresh                Refresh access token
POST   /auth/logout                 Revoke refresh token

GET    /jobs                        List jobs (pagination, filtering)
POST   /jobs                        Create a job
GET    /jobs/{id}                   Get job details
PUT    /jobs/{id}                   Update a job
DELETE /jobs/{id}                   Soft-delete a job
POST   /jobs/{id}/trigger           Trigger job execution manually
PATCH  /jobs/{id}/toggle            Enable / disable a job

GET    /jobs/{id}/executions        Execution history for a job
GET    /executions                  All execution logs (filtered, paginated)
GET    /executions/{id}             Execution log details

GET    /alerts                      Active (unacknowledged) alerts
POST   /alerts/{id}/acknowledge     Acknowledge an alert

GET    /health                      Health check
```

### worker-service (port 8000)

```
POST   /execute                     Execute a job (synchronous, returns result)
GET    /results/{job_id}/latest     Latest result for a job
GET    /health                      Health check
```

### analytics-service (port 8001)

```
GET    /stats/summary               Total jobs, success rate, errors (last 24h)
GET    /stats/daily                 Daily execution counts (last 30 days)
GET    /stats/jobs/{id}/performance Average duration and success rate for a job
GET    /stats/top-failing           Jobs with the most failures
GET    /reports/export              Export execution history as CSV

GET    /health                      Health check
```

---

## Kubernetes Deployment

### 1. Build Docker images

```bash
docker build -t process-monitor/orchestrator-service:latest ./orchestrator-service
docker build -t process-monitor/worker-service:latest ./worker-service
docker build -t process-monitor/analytics-service:latest ./analytics-service
```

### 2. Apply manifests

```bash
# Secrets (edit k8s/secrets.yaml with real values first)
kubectl apply -f k8s/secrets.yaml

# Databases and persistent storage
kubectl apply -f k8s/databases.yaml

# Application services
kubectl apply -f k8s/orchestrator.yaml
kubectl apply -f k8s/worker.yaml
kubectl apply -f k8s/analytics.yaml

# ETL job (runs every hour)
kubectl apply -f k8s/etl-job.yaml

# Autoscaling for worker-service
kubectl apply -f k8s/hpa.yaml
```

### 3. Verify

```bash
kubectl get pods
kubectl get services
kubectl get hpa
```

---

## Local Development

### orchestrator-service (Java)

```bash
cd orchestrator-service
# Set environment variables or use application-local.yml
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/orchestrator_db
export SPRING_DATASOURCE_USERNAME=orchestrator
export SPRING_DATASOURCE_PASSWORD=changeme
export JWT_SECRET=change-this-to-a-very-long-random-secret-key-at-least-32-chars
mvn spring-boot:run
```

### worker-service (Python)

```bash
cd worker-service
python -m venv venv
source venv/bin/activate   # Windows: venv\Scripts\activate
pip install -r requirements.txt
export DATABASE_URL=mysql+pymysql://worker:changeme@localhost:3306/worker_db
uvicorn app.main:app --reload --port 8000
```

### analytics-service (Python)

```bash
cd analytics-service
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
export ANALYTICS_DB_URL=mysql+pymysql://analytics:changeme@localhost:3307/analytics_db
export ORCHESTRATOR_DB_URL=postgresql://orchestrator:changeme@localhost:5432/orchestrator_db
uvicorn app.main:app --reload --port 8001
```

---

## Testing

### Java (orchestrator-service)

```bash
cd orchestrator-service
mvn test
mvn verify   # includes integration tests
```

### Python (worker-service / analytics-service)

```bash
cd worker-service
pytest

cd ../analytics-service
pytest
```

---

## Environment Variables

| Variable                      | Description                                   | Default                            |
|-------------------------------|-----------------------------------------------|------------------------------------|
| `POSTGRES_USER`               | PostgreSQL username                           | `orchestrator`                     |
| `POSTGRES_PASSWORD`           | PostgreSQL password                           | —                                  |
| `POSTGRES_DB`                 | PostgreSQL database name                      | `orchestrator_db`                  |
| `MYSQL_USER`                  | MySQL username (worker)                       | `worker`                           |
| `MYSQL_PASSWORD`              | MySQL password (worker)                       | —                                  |
| `MYSQL_DATABASE`              | MySQL database name (worker)                  | `worker_db`                        |
| `MYSQL_ANALYTICS_USER`        | MySQL username (analytics)                    | `analytics`                        |
| `MYSQL_ANALYTICS_PASSWORD`    | MySQL password (analytics)                    | —                                  |
| `MYSQL_ANALYTICS_DATABASE`    | MySQL database name (analytics)               | `analytics_db`                     |
| `JWT_SECRET`                  | JWT signing secret (min 32 characters)        | —                                  |
| `JWT_EXPIRY_MINUTES`          | Access token lifetime in minutes              | `15`                               |
| `JWT_REFRESH_EXPIRY_DAYS`     | Refresh token lifetime in days                | `7`                                |
| `ALLOWED_ORIGINS`             | Comma-separated allowed CORS origins          | `http://localhost:3000`            |
| `MAIL_HOST`                   | SMTP server host                              | `smtp.gmail.com`                   |
| `MAIL_PORT`                   | SMTP server port                              | `587`                              |
| `MAIL_USERNAME`               | SMTP account username                         | —                                  |
| `MAIL_PASSWORD`               | SMTP account password / app password          | —                                  |
| `WORKER_SERVICE_URL`          | Internal URL of the worker-service            | `http://worker-service:8000`       |
| `ORCHESTRATOR_DB_URL`         | Full connection URL to orchestrator database  | —                                  |
| `ANALYTICS_DB_URL`            | Full connection URL to analytics database     | —                                  |

---

## Features

- **Job management** — create, schedule (cron), enable/disable, and soft-delete automated process jobs
- **Job types** — `HTTP_CALL`, `CSV_PROCESS`, `DATA_VALIDATE`, `REPORT_GENERATE`
- **Execution engine** — configurable timeout (default 30s, max 300s) and retry logic with exponential backoff (5s, 15s, 30s)
- **Execution history** — full logs per execution with status, output, error, and correlation ID
- **Log retention** — automatic cleanup of logs older than 90 days
- **Alerts** — triggered on failure or max retries reached; email and webhook notification channels
- **Analytics** — daily stats, per-job performance, top-failing jobs, CSV export
- **ETL pipeline** — hourly Kubernetes CronJob aggregates operational data into the analytics database
- **Horizontal scaling** — Kubernetes HPA scales worker-service from 1 to 5 replicas based on CPU/memory
- **Observability** — structured JSON logs, correlation ID propagated across all services via `X-Correlation-Id`

---

## Security

- **Authentication** — JWT access tokens (15 min) + refresh tokens (7 days); refresh tokens are revocable
- **Passwords** — BCrypt with strength 12
- **Authorisation** — role-based access control (`ADMIN`, `OPERATOR`); operators cannot modify other operators' jobs
- **Rate limiting** — `/auth/login` and `/auth/register` capped at 10 requests/minute per IP
- **CORS** — allowed origins configured via `ALLOWED_ORIGINS` environment variable
- **Network isolation** — inter-service communication over internal Docker/K8s network only
- **Config sanitisation** — job config JSON is sanitised before persistence to prevent injection
- **Secrets management** — all credentials loaded from environment variables or Kubernetes Secrets; no hardcoded values

---

## Repository Structure

```
process-automation-monitor/
├── orchestrator-service/    # Java 17 / Spring Boot
│   ├── pom.xml
│   └── src/
│       ├── main/java/
│       │   ├── model/       # Job, ExecutionLog, Alert, User
│       │   ├── repository/  # JPA repositories
│       │   ├── service/     # Business logic
│       │   ├── scheduler/   # Quartz / @Scheduled
│       │   ├── controller/  # REST endpoints
│       │   └── security/    # JWT, Spring Security
│       └── test/
├── worker-service/          # Python 3.11 / FastAPI
│   ├── requirements.txt
│   └── app/
│       ├── main.py
│       ├── models.py
│       └── executor/        # http_task, csv_task, validate_task, report_task
├── analytics-service/       # Python 3.11 / FastAPI
│   ├── requirements.txt
│   └── app/
│       ├── main.py
│       └── etl/             # etl_job.py
├── k8s/
│   ├── secrets.yaml
│   ├── databases.yaml
│   ├── orchestrator.yaml
│   ├── worker.yaml
│   ├── analytics.yaml
│   ├── etl-job.yaml
│   └── hpa.yaml
├── docker-compose.yml
├── .env.example
├── SPECIFICATION.md
└── README.md
```
