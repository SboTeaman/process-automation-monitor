# Process Automation Monitor - Kompletny Opis Systemu

## 🎯 Cel Systemu

**Process Automation Monitor (PAM)** to zaawansowany system do zarządzania, planowania i monitorowania zautomatyzowanych procesów biznesowych. System pozwala na:

- Definiowanie złożonych zadań automatycznych (jobów)
- Planowanie ich wykonania poprzez wyrażenia cron
- Monitorowanie statusu i wyników w czasie rzeczywistym
- Zbieranie i analizowanie danych o wykonaniach
- Automatyczne powiadomienia o błędach

---

## 📋 Architektura Systemu

```
┌─────────────────────────────────────────────────────────────────────┐
│                    WEB UI (React + TypeScript)                      │
│              http://localhost:3000 (port frontend)                  │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ Login → Dashboard → Jobs → Analytics → Alerts → History     │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
                                ↓ (REST API + JWT)
┌─────────────────────────────────────────────────────────────────────┐
│                    API Gateway / Proxy (nginx)                      │
└─────────────────────────────────────────────────────────────────────┘
                                ↓
┌────────────────────────────────────────────────────────────────────┐
│  orchestrator-service (Java Spring Boot)  ← Główny kontroler      │
│  http://localhost:8080                                             │
│  • Zarządzanie jobami (CRUD)                                       │
│  • Scheduling (Quartz)                                             │
│  • Autentykacja (JWT)                                              │
│  • Zarządzanie użytkownikami                                       │
│  • PostgreSQL Database                                             │
└────────────────────────────────────────────────────────────────────┘
       ↓                              ↓                      ↓
   ┌───────────────────┐      ┌──────────────────┐    ┌──────────┐
   │ worker-service    │      │analytics-service │    │ Alerts   │
   │ (Python FastAPI)  │      │(Python FastAPI)  │    │ System   │
   │ http://8000       │      │ http://8001      │    │(Email/WH)│
   │                   │      │                  │    └──────────┘
   │ • HTTP zadania    │      │ • ETL Pipeline   │
   │ • CSV processing  │      │ • Statystyki     │
   │ • Validations     │      │ • Raporty        │
   │ • MySQL DB        │      │ • MySQL DB       │
   └───────────────────┘      └──────────────────┘
```

---

## 🏗️ Komponenty Systemu

### 1. **Frontend (React + TypeScript + Tailwind)**
**Lokalizacja:** `/frontend`
**Port:** 3000

#### Funkcjonalności:
- **Autentykacja** - Logowanie i rejestracja z JWT
- **Dashboard** - Przegląd systemu, statystyki
- **Zarządzanie jobami** - Tworzenie, edycja, usuwanie zadań
- **Monitoring** - Wykonania w czasie rzeczywistym
- **Analityka** - Wykresy, statystyki, raporty
- **Alerty** - Zarządzanie powiadomieniami
- **Historia** - Logi wszystkich wykonań

#### Technologia:
```
- React 19 + TypeScript
- React Router v6 (routing)
- Zustand (state management)
- Axios (HTTP + JWT interceptor)
- Recharts (wykresy)
- Tailwind CSS (styling)
- Lucide React (ikony)
- date-fns (formatowanie dat)
```

---

### 2. **Orchestrator Service (Java Spring Boot)**
**Lokalizacja:** `/orchestrator-service`
**Port:** 8080
**Database:** PostgreSQL

#### Odpowiedzialność:
- **Główna logika biznesowa systemu**
- Zarządzanie jobami (CRUD operacje)
- Scheduling tasków poprzez Quartz
- Autentykacja i autoryzacja (JWT + Spring Security)
- Role-based access control (ADMIN, OPERATOR)
- Zarządzanie alertami

#### Główne Endpointy:
```
AUTH:
POST   /auth/register              - Rejestracja
POST   /auth/login                 - Login
POST   /auth/refresh               - Refresh token
POST   /auth/logout                - Logout

JOBS:
GET    /jobs                       - Lista jobów (pagination, filter)
POST   /jobs                       - Tworzenie nowego joba
GET    /jobs/{id}                  - Szczegóły joba
PUT    /jobs/{id}                  - Aktualizacja joba
DELETE /jobs/{id}                  - Usuwanie joba (soft delete)
POST   /jobs/{id}/trigger          - Ręczne wyzwolenie joba
PATCH  /jobs/{id}/toggle           - Włączenie/wyłączenie joba

EXECUTIONS:
GET    /executions                 - Historia wykonań (paginacja)
GET    /executions/{id}            - Szczegóły wykonania
GET    /jobs/{id}/executions       - Historia konkretnego joba

ALERTS:
GET    /alerts                     - Aktywne alerty
POST   /alerts/{id}/acknowledge    - Potwierdzenie alertu

HEALTH:
GET    /health                     - Health check
```

#### Technologia:
```
- Java 17
- Spring Boot 3
- Spring Security + JWT
- Quartz (scheduling)
- JPA (ORM)
- PostgreSQL
- Maven
- Swagger/OpenAPI
```

---

### 3. **Worker Service (Python FastAPI)**
**Lokalizacja:** `/worker-service`
**Port:** 8000
**Database:** MySQL

#### Odpowiedzialność:
- **Wykonywanie rzeczywistych tasków**
- HTTP callsy do zewnętrznych systemów
- Przetwarzanie plików CSV
- Walidacja danych
- Generowanie raportów
- Logowanie rezultatów

#### Typy Jobów:
```
1. HTTP_CALL
   - Wysłanie requesta HTTP do URL
   - Obsługa różnych metod (GET, POST, PUT, DELETE)
   - Konfiguracja headersów i bodyu

2. CSV_PROCESS
   - Wczytanie pliku CSV
   - Transformacja danych
   - Zapis wyników

3. DATA_VALIDATE
   - Walidacja danych wg reguł
   - Sprawdzenie formatów
   - Raportowanie błędów

4. REPORT_GENERATE
   - Generowanie raportów z danych
   - Export do różnych formatów
   - Wysyłanie raportów
```

#### Główne Endpointy:
```
POST   /execute                    - Wykonaj job (synchronicznie)
GET    /results/{job_id}/latest    - Ostatni rezultat joba
GET    /health                     - Health check
```

#### Technologia:
```
- Python 3.11
- FastAPI (async framework)
- SQLAlchemy (ORM)
- MySQL
- Alembic (migrations)
```

---

### 4. **Analytics Service (Python FastAPI)**
**Lokalizacja:** `/analytics-service`
**Port:** 8001
**Database:** MySQL

#### Odpowiedzialność:
- **Zbieranie i analiza danych z wykonań**
- ETL Pipeline (godzinnie)
- Generowanie statystyk
- Raportowanie trendów
- Identyfikacja problemów

#### Główne Endpointy:
```
STATS:
GET    /stats/summary              - Podsumowanie (24h)
                                     - Total jobs
                                     - Success rate
                                     - Recent errors
                                     
GET    /stats/daily                - Daily counts (30 dni)

GET    /stats/jobs/{id}/performance - Performance joba
                                      - Avg duration
                                      - Success rate

GET    /stats/top-failing          - Top failing jobs

REPORTS:
GET    /reports/export             - Export CSV

HEALTH:
GET    /health                     - Health check
```

#### Technologia:
```
- Python 3.11
- FastAPI
- SQLAlchemy
- MySQL
- Alembic
```

---

## 🔄 Przepływ Pracy

### Scenariusz 1: Tworzenie i Wykonanie Joba

```
1. USER (Frontend)
   └─→ Otwiera aplikację na http://localhost:3000
   └─→ Loguje się (JWT token)
   └─→ Przechodzi do "Jobs"

2. USER TWORZY JOB
   └─→ Kliknie "Create Job"
   └─→ Wypełnia formularz:
       - Name: "Sync User Data"
       - Type: "HTTP_CALL"
       - Cron: "0 2 * * *" (codziennie o 2:00)
       - Config:
         {
           "url": "https://api.example.com/sync",
           "method": "POST",
           "headers": {"Authorization": "Bearer token"}
         }
   └─→ Frontend wysyła POST /jobs do orchestrator

3. ORCHESTRATOR ODBIERA REQUEST
   └─→ Waliduje dane
   └─→ Sprawdza JWT token
   └─→ Zapisuje job w PostgreSQL
   └─→ Rejestruje w Quartz Schedulerze
   └─→ Zwraca job ID

4. SCHEDULING
   └─→ Quartz czeka na moment 2:00
   └─→ O 2:00 wyzwala callback
   └─→ Orchestrator wysyła request do worker-service

5. WORKER WYKONUJE
   └─→ Odbiera POST /execute
   └─→ Wykonuje HTTP call do https://api.example.com/sync
   └─→ Zapisuje rezultat w MySQL
   └─→ Zwraca status SUCCESS/FAILED

6. ORCHESTRATOR REJESTRUJE WYNIK
   └─→ Zapisuje log wykonania w PostgreSQL
   └─→ Jeśli ERROR: tworzy Alert
   └─→ Wysyła email/webhook do użytkownika (jeśli skonfigurowany)

7. ANALYTICS AGREGUJE DANE
   └─→ Co godzinę (CronJob w K8s)
   └─→ Czyta logi z orchestrator
   └─→ Agreguje do tabel w MySQL
   └─→ Oblicza statystyki

8. USER MONITORUJE
   └─→ Przechodzi do Dashboard
   └─→ Widzi success rate, liczby błędów
   └─→ Przechodzi do Analytics
   └─→ Widzi wykresy wydajności
   └─→ Przechodzi do Execution History
   └─→ Widzi szczegóły każdego wykonania
```

---

## 🔐 Bezpieczeństwo

### Autentykacja
```
1. User loguje się: POST /auth/login
   └─→ Orchestrator waliduje hasło (BCrypt 12)
   └─→ Generuje JWT token (15 min validity)
   └─→ Generuje refresh token (7 dni validity)
   └─→ Zwraca oba tokeny

2. User wysyła request z JWT w Authorization header
   └─→ Axios interceptor automatycznie dodaje header
   └─→ Spring Security waliduje token

3. Token wygasa po 15 minut
   └─→ Axios interceptor przechwytuje 401
   └─→ Automatycznie wysyła refresh token
   └─→ Pobiera nowy access token
   └─→ Powtarza oryginalny request

4. User loguje się out
   └─→ Refresh token jest unieważniany
   └─→ Frontend usuwa tokeny z localStorage
```

### Autoryzacja
```
Dwie role:
- ADMIN: Full access, może zarządzać innymi użytkownikami
- OPERATOR: Może zarządzać tylko swoimi jobami

Walidacja na każdym endpoincie:
- Job CRUD: User musi być owner lub ADMIN
- Settings: Tylko ADMIN
```

### Ochrona Danych
```
- Hasła: BCrypt 12
- Secrets: Zmienne środowiskowe
- Config sanitizacja: SQL injection protection
- Rate limiting: 10 req/min na /login i /register
- CORS: Tylko white-listed origins
```

---

## 💾 Bazy Danych

### PostgreSQL (Orchestrator)
```sql
Tables:
- users (id, username, email, password_hash, role, created_at)
- jobs (id, user_id, name, description, type, cron_expression, 
        config_json, timeout, max_retries, enabled, created_at)
- execution_logs (id, job_id, status, output, error, duration_ms,
                  executed_at, correlation_id)
- alerts (id, job_id, type, message, acknowledged, created_at)
- refresh_tokens (id, user_id, token_hash, expires_at, revoked_at)

Indexes:
- jobs(user_id, enabled)
- execution_logs(job_id, executed_at)
- execution_logs(status, executed_at)
```

### MySQL (Worker)
```sql
Tables:
- job_results (job_id, result_json, created_at)

Indexes:
- job_results(job_id, created_at)
```

### MySQL (Analytics)
```sql
Tables:
- daily_stats (date, total_jobs, success_count, failure_count, avg_duration_ms)
- job_performance (job_id, success_rate, avg_duration_ms, failure_count, last_updated)
- hourly_execution_counts (hour, job_id, count, total_duration_ms)

Indexes:
- daily_stats(date)
- job_performance(job_id)
```

---

## ⚙️ Konfiguracja

### Frontend (.env)
```env
VITE_API_URL=http://localhost:8080
```

### Orchestrator (application.yml)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/orchestrator_db
    username: orchestrator
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
jwt:
  secret: ${JWT_SECRET}  # Min 32 chars
  expiry-minutes: 15
  refresh-expiry-days: 7
cors:
  allowed-origins: http://localhost:3000
mail:
  enabled: true
  smtp-host: smtp.gmail.com
  smtp-port: 587
```

### Worker (environment)
```env
DATABASE_URL=mysql+pymysql://worker:password@localhost:3306/worker_db
ORCHESTRATOR_URL=http://orchestrator-service:8080
```

### Analytics (environment)
```env
ORCHESTRATOR_DB_URL=postgresql://user:pass@orchestrator:5432/orchestrator_db
ANALYTICS_DB_URL=mysql+pymysql://analytics:pass@mysql:3307/analytics_db
ETL_RUN_INTERVAL=3600  # 1 godzina
```

---

## 🚀 Deployment

### Development (Docker Compose)
```bash
docker-compose up -d
```

Dostęp:
- Frontend: http://localhost:3000
- Orchestrator API: http://localhost:8080/swagger-ui.html
- Worker API: http://localhost:8000/docs
- Analytics API: http://localhost:8001/docs

### Production (Kubernetes)
```bash
# Build images
docker build -t process-monitor/orchestrator:latest ./orchestrator-service
docker build -t process-monitor/worker:latest ./worker-service
docker build -t process-monitor/analytics:latest ./analytics-service

# Deploy
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/databases.yaml
kubectl apply -f k8s/orchestrator.yaml
kubectl apply -f k8s/worker.yaml
kubectl apply -f k8s/analytics.yaml
kubectl apply -f k8s/etl-job.yaml
kubectl apply -f k8s/hpa.yaml
```

---

## 📊 Przykładowe Przypadki Użycia

### Case 1: Synchronizacja Danych z CRM
```
Job: "Daily CRM Sync"
Type: HTTP_CALL
Schedule: 0 3 * * * (3:00 AM codziennie)
Config:
{
  "url": "https://crm.company.com/api/export",
  "method": "POST",
  "timeout": 60,
  "maxRetries": 3
}

Rezultat:
- Każdego ranka ściąga dane z CRM
- Jeśli fails: wysyła alert na email admina
- Analytics zbiera metrics: success rate, avg duration
- Dashboard pokazuje czy ostatnia sync się powiodła
```

### Case 2: Walidacja Importu Pliku
```
Job: "Weekly Data Validation"
Type: DATA_VALIDATE
Schedule: 0 4 * * 1 (Poniedziałek 4:00 AM)
Config:
{
  "fileSource": "s3://bucket/weekly-import.csv",
  "rules": [
    {"field": "email", "type": "email"},
    {"field": "amount", "type": "numeric", "min": 0}
  ]
}

Rezultat:
- Każdego poniedziałku waliduje import
- Raportuje błędy w formacie JSON
- Analytics śledzi trend błędów
- Alerts jeśli >10% wierszy ma błędy
```

### Case 3: Generowanie Raportu Sprzedaży
```
Job: "Monthly Sales Report"
Type: REPORT_GENERATE
Schedule: 0 9 1 * * (1. każdego miesiąca o 9:00)
Config:
{
  "reportType": "sales_summary",
  "dateRange": "current_month",
  "recipients": ["sales-team@company.com"]
}

Rezultat:
- Generuje raport z danych z poprzedniego miesiąca
- Wysyła emailem do zespołu
- Archiwizuje w systemie
- Dashboard pokazuje historię raportów
```

---

## 📈 Monitoring i Alerting

### Metryki Śledzone
```
- Total jobs: Ilość wszystkich jobów
- Success rate: % pomyślnych wykonań (24h)
- Avg duration: Średni czas wykonania
- Error rate: % błędów (24h)
- Last execution: Ostatnie wykonanie
- Top failing jobs: Joby z największą liczbą błędów
```

### Typy Alertów
```
1. Job Failure Alert
   - Trigger: Job execution failed
   - Action: Email, Webhook, Dashboard notification

2. Max Retries Exceeded
   - Trigger: Job failed all 3 retry attempts
   - Severity: HIGH
   - Action: Immediate notification

3. Performance Degradation
   - Trigger: Avg duration > 2x historical avg
   - Severity: MEDIUM
   - Action: Alert to DevOps team

4. Overdue Job
   - Trigger: Scheduled job didn't run
   - Severity: CRITICAL
   - Action: Immediate alert
```

---

## 🔧 Skalowanie

### Horizontal Scaling
```
Worker Service:
- Kubernetes HPA: Min 1, Max 5 replicas
- Scale trigger: CPU > 70% lub Memory > 80%
- Useful dla heavy-duty jobs

Orchestrator Service:
- Stateless aplikacja
- Można runować na wielu podach
- Session state przechowywane w JWT (not in memory)

Analytics Service:
- Read-only access
- Można skalować dla heavy queries
```

### Vertical Scaling
```
Databases:
- PostgreSQL: Storage monitoring + automatic backups
- MySQL: Connection pooling + slow query log

Services:
- JVM tuning dla Java services
- Python workers: async/await optimization
```

---

## 🐛 Troubleshooting

### Frontend nie ładuje się
```
1. Sprawdź czy http://localhost:3000 odpowiada
2. Sprawdź console w DevTools (F12)
3. Sprawdź czy backend na 8080 odpowiada
4. Clear browser cache (Ctrl+Shift+Del)
```

### Job nie wykonuje się
```
1. Sprawdź czy job jest enabled (toggle w Jobs)
2. Sprawdź cron expression: https://crontab.guru
3. Sprawdź czy worker-service jest alive (GET /health)
4. Sprawdź Execution History czy są błędy
```

### API zwraca 401 Unauthorized
```
1. Login znowu - token może wygasnąć
2. Sprawdź czy JWT_SECRET w orchestrator się zmienił
3. Clear localStorage i spróbuj logować
```

### Database connection error
```
1. docker-compose ps (sprawdź czy db runnuje)
2. Sprawdź connection string w env
3. Sprawdź hasła w .env
```

---

## 📚 API Documentation

Swagger UI:
- Orchestrator: http://localhost:8080/swagger-ui.html
- Worker: http://localhost:8000/docs
- Analytics: http://localhost:8001/docs

OpenAPI Schema:
- Orchestrator: http://localhost:8080/v3/api-docs

---

## 🎓 Nauka Systemu

### Dla Developerów
1. Przeczytaj README.md w każdej usłudze
2. Sprawdź Swagger documentation
3. Uruchom na docker-compose
4. Spróbuj tworzyć joby poprzez UI
5. Monitoruj requests w DevTools

### Dla DevOps
1. Przeczytaj k8s/ manifests
2. Sprawdź Dockerfile w każdej usłudze
3. Zrozum networking między podami
4. Skonfiguruj ingress/load balancer
5. Setup monitoring (Prometheus/Grafana)

### Dla Administratorów
1. Zaloguj się jako ADMIN
2. Przeczytaj Security section
3. Skonfiguruj email alerts
4. Setup backup strategy
5. Monitor performance metrics

---

## 📝 Podsumowanie

**Process Automation Monitor** to kompletny system do automatyzacji procesów biznesowych z:

✅ Nowoczesnym webowym interfejsem (React)
✅ Niezawodnym backendem (Java + Python)
✅ Zaawansowanym schedulingiem (Quartz)
✅ Analityką i raportowaniem
✅ Bezpieczeństwem (JWT, BCrypt)
✅ Skalowalnością (Kubernetes ready)
✅ Monitoringiem i alertingiem

System jest gotów do produkcji i może obsługiwać tysiące jobów na dużej skali.
