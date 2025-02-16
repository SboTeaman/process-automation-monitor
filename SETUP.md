# Local Development Setup

This guide will help you set up the Process Automation Monitor for local development.

## Prerequisites

- **Docker & Docker Compose** (for containerized development)
- **Node.js 18+** (for frontend development)
- **Java 17** (for local orchestrator development - optional)
- **Python 3.11** (for local worker/analytics development - optional)

## Quick Start (Docker Compose)

The easiest way to run the entire stack locally is with Docker Compose.

### 1. Generate Environment Variables

**On Linux/macOS:**
```bash
./setup-env.sh
```

**On Windows (PowerShell):**
```powershell
.\setup-env.ps1
```

Or manually create `.env` from `.env.example`:
```bash
cp .env.example .env
# Edit .env and fill in secrets (use: openssl rand -base64 32)
```

### 2. Start All Services

```bash
docker-compose up -d
```

This starts:
- **Frontend** (React) on http://localhost:3000
- **Orchestrator** (Java/Spring Boot) on http://localhost:8080
- **Worker Service** (Python/FastAPI) on http://localhost:8000
- **Analytics Service** (Python/FastAPI) on http://localhost:8001
- **PostgreSQL** for orchestrator
- **MySQL** for worker and analytics

### 3. Verify Services

Check health endpoints:
```bash
curl http://localhost:8080/health     # Orchestrator
curl http://localhost:8000/health     # Worker
curl http://localhost:8001/health     # Analytics
```

### 4. Use the Application

1. Open http://localhost:3000 in your browser
2. Click **Register** and create an account
3. Go to **Jobs > Create Job** to set up your first scheduled task
4. Monitor execution in **Execution History** and **Analytics**

## Frontend-Only Development

If you only want to work on the frontend:

```bash
cd frontend
npm install
npm run dev        # http://localhost:3000
```

The frontend will make API calls to `http://localhost:8080`, so the backend services should still be running.

### Build for Production

```bash
cd frontend
npm run build      # Creates optimized bundle in dist/
npm run preview    # Test the production build locally
```

### Linting

```bash
npm run lint       # Check for code style issues
```

Current status: **10 warnings** (mostly about large bundle size and `any` types in error handlers - these are safe to ignore for now).

## Backend Development (without Docker)

### Orchestrator Service (Java/Spring Boot)

```bash
cd process-automation-monitor/orchestrator-service
# Requires Java 17 and Maven installed
./mvnw spring-boot:run
# Available at http://localhost:8080
```

### Worker Service (Python/FastAPI)

```bash
cd process-automation-monitor/worker-service
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
# Available at http://localhost:8000
```

### Analytics Service (Python/FastAPI)

```bash
cd process-automation-monitor/analytics-service
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8001
# Available at http://localhost:8001
```

**Note:** Without Docker, you'll need to manually set up PostgreSQL and MySQL databases with the environment variables from `.env`.

## Running Tests

### Frontend Tests

Currently, the project doesn't have frontend unit tests configured. You can add them with:

```bash
npm install -D vitest @testing-library/react @testing-library/jest-dom
```

### Backend Tests

**Python services:**
```bash
cd process-automation-monitor/worker-service
pytest

cd process-automation-monitor/analytics-service
pytest
```

**Java service:**
```bash
cd process-automation-monitor/orchestrator-service
./mvnw test
```

## Troubleshooting

### Port Already in Use

If a port is already in use, you can:
1. Stop the conflicting service: `docker-compose down`
2. Or change the port in `docker-compose.yml` and `.env`

### Database Connection Errors

Make sure all services are healthy:
```bash
docker-compose ps
docker logs orchestrator-postgres
docker logs worker-mysql
docker logs analytics-mysql
```

### JWT Token Issues

If authentication fails, verify:
1. `JWT_SECRET` is set in `.env`
2. Tokens are being refreshed (check browser console for 401 errors)
3. Clear browser cookies and re-login

### Docker Cleanup

To remove everything and start fresh:
```bash
docker-compose down -v    # Remove volumes too
rm .env
./setup-env.sh
docker-compose up -d
```

## Environment Variables

See `.env.example` for all available configuration options. Key variables:

- `JWT_SECRET` - Secret key for signing JWT tokens
- `WORKER_API_KEY` - API key for worker service authentication
- `ANALYTICS_API_KEY` - API key for analytics service authentication
- `POSTGRES_PASSWORD` - PostgreSQL password
- `MYSQL_PASSWORD` - MySQL passwords
- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` - Email configuration (optional)
- `SWAGGER_ENABLED` - Enable/disable OpenAPI documentation

## Code Quality

The project enforces code quality through:

- **ESLint** (TypeScript/React)
- **Prettier** (code formatting)
- **Spring Security** (Java)
- **PyTest** (Python)

Run checks before committing:

```bash
cd frontend
npm run lint      # Check for issues
npm run build     # Verify build succeeds
```

## Next Steps

- Read the [README.md](README.md) for architecture and feature overview
- Check the API documentation at http://localhost:8080/swagger-ui.html (if `SWAGGER_ENABLED=true`)
- Explore the [project structure](process-automation-monitor/) for code organization
- Review security documentation in `k8s/sealed-secrets-setup.md`

## Getting Help

- Check service logs: `docker-compose logs <service-name>`
- Inspect environment: `cat .env`
- Review git history: `git log --oneline`
