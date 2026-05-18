# Process Automation Monitor — Specyfikacja wymagań

## 1. Opis systemu

System do definiowania, planowania i monitorowania automatycznych zadań procesowych. Użytkownik (np. administrator IT lub analityk) rejestruje zadania, system wykonuje je cyklicznie lub na żądanie, a wyniki i logi są dostępne przez API i historię wykonań.

Projekt nawiązuje do działalności firmy **Craftware** — Platynowego Partnera UiPath, specjalizującego się w automatyzacji procesów (RPA) oraz wdrożeniach enterprise dla klientów takich jak Allegro, Orange, Generali.

---

## 2. Stack technologiczny

| Komponent | Technologia |
|---|---|
| `orchestrator-service` | Java 17, Spring Boot, Maven, PostgreSQL |
| `worker-service` | Python 3.11, FastAPI, MySQL |
| Konteneryzacja | Docker, Docker Compose |
| Orkiestracja | Kubernetes (Deployment, CronJob, HPA, ConfigMap) |
| Kontrola wersji | Git (branching: `main`, `dev`, `feature/*`) |
| Dokumentacja API | Swagger/OpenAPI (Java), FastAPI autodocs (Python) |
| Migracje baz danych | Flyway (Java), Alembic (Python) |

---

## 3. Architektura systemu

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Docker Compose / K8s                          │
│                                                                     │
│  ┌───────────────────────┐        ┌────────────────────────┐        │
│  │   orchestrator-service │        │    worker-service      │        │
│  │   Java 17              │──REST──►   Python 3.11          │        │
│  │   Spring Boot          │◄──────│   FastAPI              │        │
│  │   Maven                │        │                        │        │
│  │   PostgreSQL           │        │   MySQL                │        │
│  └──────────┬────────────┘        └────────────────────────┘        │
│             │                                                        │
│             │ (ETL / scheduled sync)                                 │
│             ▼                                                        │
│  ┌───────────────────────┐                                           │
│  │   analytics-service   │                                           │
│  │   Python 3.11         │                                           │
│  │   FastAPI             │                                           │
│  │   MySQL               │                                           │
│  └──────────┬────────────┘                                           │
│             │                                                        │
│     ┌───────▼────────────────────────────────┐                      │
│     │           API Gateway / nginx           │                      │
│     └─────────────────────────────────────────┘                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Przepływ danych

```
Użytkownik definiuje zadanie
        ↓
orchestrator-service (Java) — zapisuje, planuje, zarządza
        ↓
worker-service (Python) — wykonuje zadanie (HTTP call, CSV, walidacja)
        ↓
Wynik zapisany do bazy PostgreSQL + log wykonania
        ↓
ETL job (CronJob K8s, co 1h) — agreguje dane do MySQL (analytics)
        ↓
analytics-service — statystyki, raporty, alerty
```

---

## 4. Struktura repozytorium

```
process-automation-monitor/
├── orchestrator-service/           # Java/Spring Boot
│   ├── pom.xml
│   └── src/
│       ├── main/java/
│       │   ├── model/              # Job, ExecutionLog, Alert, User
│       │   ├── repository/         # JPA repositories
│       │   ├── service/            # logika biznesowa
│       │   ├── scheduler/          # Quartz / @Scheduled
│       │   ├── controller/         # REST endpoints
│       │   └── security/           # JWT, Spring Security
│       └── test/
├── worker-service/                 # Python/FastAPI
│   ├── requirements.txt
│   └── app/
│       ├── main.py
│       ├── models.py
│       ├── executor/
│       │   ├── http_task.py
│       │   ├── csv_task.py
│       │   ├── validate_task.py
│       │   └── report_task.py
│       └── routes/
├── docker-compose.yml
├── k8s/
│   ├── orchestrator.yaml
│   ├── worker.yaml
│   ├── analytics.yaml
│   ├── cronjob.yaml          # wyzwalacz zadań (K8s CronJob)
│   ├── etl-job.yaml          # agregacja PostgreSQL → MySQL (co 1h)
│   ├── databases.yaml
│   ├── secrets.yaml          # K8s Secrets (hasła, klucze JWT)
│   └── hpa.yaml
├── .env.example
├── SPECIFICATION.md
└── README.md
```

---

## 5. Aktorzy systemu

| Aktor | Opis |
|---|---|
| **Admin** | Zarządza zadaniami, użytkownikami, konfiguracją systemu |
| **Operator** | Tworzy i monitoruje zadania, przegląda logi |
| **System (Scheduler)** | Automatycznie wyzwala zadania według harmonogramu |
| **Worker** | Wewnętrzny komponent wykonujący zadania (Python) |

---

## 6. Wymagania funkcjonalne

### 6.1 Zarządzanie użytkownikami (`orchestrator-service`)

| ID | Wymaganie |
|---|---|
| F-01 | Rejestracja konta (email + hasło) |
| F-02 | Logowanie — zwraca JWT token (access + refresh) |
| F-03 | Role: `ADMIN`, `OPERATOR` — różne uprawnienia |
| F-04 | Zmiana hasła, profil użytkownika |

---

### 6.2 Zarządzanie zadaniami — Jobs (`orchestrator-service`)

| ID | Wymaganie |
|---|---|
| F-10 | Tworzenie zadania z polami: `name`, `type`, `config`, `schedule`, `enabled` |
| F-11 | Typy zadań: `HTTP_CALL`, `CSV_PROCESS`, `DATA_VALIDATE`, `REPORT_GENERATE` |
| F-12 | Harmonogram w formacie **cron expression** (np. `0 8 * * MON`) |
| F-13 | Włączanie / wyłączanie zadania bez usuwania |
| F-14 | Edycja i usuwanie zadania (soft delete) |
| F-15 | Listowanie zadań z filtrowaniem po typie, statusie, autorze |
| F-16 | Ręczne uruchomienie zadania poza harmonogramem (`POST /jobs/{id}/trigger`) |
| F-17 | Paginacja i sortowanie listy zadań |

**Model zadania:**

```json
{
  "id": "uuid",
  "name": "string",
  "type": "HTTP_CALL | CSV_PROCESS | DATA_VALIDATE | REPORT_GENERATE",
  "config": {},
  "schedule": "0 8 * * MON",
  "timezone": "Europe/Warsaw",
  "enabled": true,
  "timeout": 30,
  "maxRetries": 3,
  "createdBy": "userId",
  "createdAt": "datetime",
  "updatedAt": "datetime",
  "deletedAt": "datetime | null",
  "lastRunAt": "datetime",
  "lastStatus": "SUCCESS | FAILED | RUNNING | PENDING"
}
```

**Schematy `config` per typ zadania:**

```json
// HTTP_CALL
{
  "url": "https://example.com/api",
  "method": "GET | POST | PUT | DELETE",
  "headers": { "Authorization": "Bearer ..." },
  "body": {},
  "expectedStatusCode": 200
}

// CSV_PROCESS
{
  "sourcePath": "/data/input.csv",
  "delimiter": ",",
  "encoding": "UTF-8",
  "rules": [
    { "column": "email", "transform": "lowercase" },
    { "column": "amount", "type": "decimal" }
  ],
  "outputPath": "/data/output.csv"
}

// DATA_VALIDATE
{
  "dataSource": "mysql | api",
  "endpoint": "string | null",
  "rules": [
    { "field": "email", "required": true, "format": "email" },
    { "field": "age", "min": 0, "max": 150 }
  ]
}

// REPORT_GENERATE
{
  "query": "SELECT ...",
  "format": "JSON | CSV",
  "outputPath": "/reports/",
  "groupBy": "day | week | month"
}
```

---

### 6.3 Wykonywanie zadań — Executor (`worker-service`)

| ID | Wymaganie |
|---|---|
| F-20 | `orchestrator-service` wysyła zlecenie do `worker-service` przez REST |
| F-21 | Worker wykonuje zadanie i zwraca wynik: `SUCCESS` / `FAILED` + payload |
| F-22 | `HTTP_CALL` — wysyła request do zewnętrznego URL, zapisuje response code i body |
| F-23 | `CSV_PROCESS` — parsuje CSV, transformuje dane wg reguł z config JSON |
| F-24 | `DATA_VALIDATE` — waliduje dane wg zdefiniowanych reguł (format, zakres, wymagane pola) |
| F-25 | `REPORT_GENERATE` — agreguje dane z MySQL, generuje JSON/CSV raport |
| F-26 | Timeout na wykonanie zadania — konfigurowalny per zadanie (pole `timeout`, domyślnie 30s, max 300s) |
| F-27 | Retry — przy błędzie ponawia `maxRetries` razy (domyślnie 3) z backoff (5s, 15s, 30s); konfigurowalny per zadanie |
| F-28 | Zadanie utknięte w statusie `RUNNING` dłużej niż `timeout * (maxRetries + 1)` sekund jest automatycznie oznaczane jako `FAILED` przez background job |

---

### 6.4 Historia wykonań — Execution Logs

| ID | Wymaganie |
|---|---|
| F-30 | Każde wykonanie tworzy rekord `ExecutionLog` |
| F-31 | Log zawiera: `jobId`, `startedAt`, `finishedAt`, `status`, `output`, `errorMessage`, `attempt` |
| F-32 | Lista logów z filtrowaniem po zadaniu, statusie, przedziale czasowym |
| F-33 | Szczegóły pojedynczego wykonania z pełnym outputem |
| F-34 | Retencja logów — automatyczne czyszczenie starszych niż 90 dni |

---

### 6.5 Alerty i powiadomienia

| ID | Wymaganie |
|---|---|
| F-40 | Konfiguracja alertu per zadanie: powiadom gdy `FAILED` lub gdy `attempt >= maxRetries` |
| F-41 | Zapis alertu do bazy (tabela `alerts`) — `triggered_at`, `job_id`, `reason`, `severity` |
| F-42 | Endpoint `GET /alerts` — lista aktywnych (niepotwierdzonych) alertów z paginacją |
| F-43 | Potwierdzenie alertu (`POST /alerts/{id}/acknowledge`) — zapisuje `acknowledged_by` i `acknowledged_at` |
| F-44 | Konfiguracja kanału powiadomień per zadanie: email (SMTP) lub webhook (HTTP POST na URL) |
| F-45 | Przy wyzwoleniu alertu system wysyła powiadomienie skonfigurowanym kanałem; niepowodzenie wysyłki nie blokuje wykonania zadania |

---

### 6.6 Dashboard / Statystyki (`analytics-service` — FastAPI)

| ID | Wymaganie |
|---|---|
| F-50 | `GET /stats/summary` — liczba zadań, % sukcesu, liczba błędów w ostatnich 24h |
| F-51 | `GET /stats/daily` — wykres wykonań dzień po dniu (ostatnie 30 dni) |
| F-52 | `GET /stats/jobs/{id}/performance` — średni czas wykonania, wskaźnik sukcesu |
| F-53 | `GET /stats/top-failing` — zadania z największą liczbą błędów |
| F-54 | `GET /reports/export` — eksport historii jako CSV |
| F-55 | Kubernetes CronJob (`etl-job`, co 1 godzinę) agreguje dane z PostgreSQL (`execution_logs`) do MySQL (`daily_stats`, `job_performance`) — zapewnia oddzielenie danych analitycznych od operacyjnych |

---

## 7. Wymagania niefunkcjonalne

### 7.1 Wydajność

| ID | Wymaganie |
|---|---|
| NF-01 | API odpowiada w < 300ms dla 95% requestów przy 50 równoczesnych użytkownikach |
| NF-02 | Scheduler obsługuje co najmniej 100 zadań cyklicznych bez degradacji |
| NF-03 | Worker obsługuje 10 równoległych wykonań (thread pool) |

### 7.2 Bezpieczeństwo

| ID | Wymaganie |
|---|---|
| NF-10 | Każdy endpoint (poza `/auth`) wymaga ważnego JWT |
| NF-11 | JWT wygasa po 15 min, refresh token po 7 dniach; refresh token można unieważnić (`POST /auth/logout`) |
| NF-12 | Hasła hashowane BCrypt (strength 12) |
| NF-13 | Operator nie może modyfikować zadań innego operatora (chyba że Admin) |
| NF-14 | Config zadania (JSON) — sanityzacja przed zapisem, brak możliwości code injection |
| NF-15 | Komunikacja między serwisami przez sieć wewnętrzną Dockera (nie publiczna) |
| NF-16 | Rate limiting na endpointach `/auth/login` i `/auth/register`: max 10 requestów/minutę per IP (ochrona przed brute force) |
| NF-17 | CORS — dozwolone origins konfigurowane przez zmienną środowiskową `ALLOWED_ORIGINS`; domyślnie tylko `localhost` |

### 7.3 Niezawodność

| ID | Wymaganie |
|---|---|
| NF-20 | Awaria `worker-service` nie powinna crashować `orchestrator-service` |
| NF-21 | Nieudane zadanie trafia do retry queue (max 3 próby) |
| NF-22 | Logi wykonań są persystowane niezależnie od stanu aplikacji |
| NF-23 | Health check endpoint (`GET /health`) dla każdego serwisu |

### 7.4 Skalowalność

| ID | Wymaganie |
|---|---|
| NF-30 | Architektura umożliwia uruchomienie wielu instancji `worker-service` |
| NF-31 | Kubernetes HorizontalPodAutoscaler dla `worker-service` (min 1, max 5 replik) |
| NF-32 | Bazy danych z persystentnymi voluminami (PersistentVolumeClaim) |

### 7.5 Obserwowalność

| ID | Wymaganie |
|---|---|
| NF-40 | Ustrukturyzowane logi (JSON format) z poziomami: `DEBUG`, `INFO`, `WARN`, `ERROR` |
| NF-41 | Każdy request loguje: `method`, `path`, `status`, `duration`, `userId`, `correlationId` |
| NF-42 | Błędy zawierają `stacktrace` w logach, ale **nie** w response API |
| NF-43 | Każde wywołanie orchestrator → worker zawiera nagłówek `X-Correlation-Id` (UUID); propagowany przez wszystkie serwisy, zapisywany w `execution_logs` — umożliwia pełne śledzenie wykonania zadania |
| NF-44 | Response API dla błędów zwraca ujednolicony format: `{ "error": "string", "code": "ERROR_CODE", "timestamp": "datetime", "correlationId": "uuid" }` |

### 7.6 Utrzymywalność

| ID | Wymaganie |
|---|---|
| NF-50 | Pokrycie testami jednostkowymi ≥ 70% dla logiki biznesowej |
| NF-51 | Swagger/OpenAPI dla `orchestrator-service`, autodocs FastAPI dla `worker-service` |
| NF-52 | Zmienne środowiskowe przez `.env` / Kubernetes ConfigMap — brak hardcodowanych secretów |
| NF-53 | Migracje bazy danych przez Flyway (Java) i Alembic (Python) |

---

## 8. Endpointy API

### orchestrator-service (Java / port 8080)

```
POST   /auth/register
POST   /auth/login
POST   /auth/refresh
POST   /auth/logout

GET    /jobs                          # lista z paginacją i filtrowaniem
POST   /jobs
GET    /jobs/{id}
PUT    /jobs/{id}
DELETE /jobs/{id}                     # soft delete (ustawia deleted=true, deletedAt)
POST   /jobs/{id}/trigger
PATCH  /jobs/{id}/toggle

GET    /jobs/{id}/executions          # logi dla konkretnego zadania (z paginacją)
GET    /executions                    # wszystkie logi (z filtrowaniem i paginacją)
GET    /executions/{id}

GET    /alerts                        # tylko niepotwierdzonych (z paginacją)
POST   /alerts/{id}/acknowledge

GET    /health
```

### worker-service (Python / port 8000)

```
POST   /execute                       # przyjmuje zlecenie, zwraca wynik synchronicznie
GET    /results/{job_id}/latest       # ostatni wynik dla danego job_id
GET    /health
```

### analytics-service (Python / port 8001)

```
GET    /stats/summary
GET    /stats/daily
GET    /stats/jobs/{id}/performance
GET    /stats/top-failing
GET    /reports/export

GET    /health
```

---

## 9. Model danych

### PostgreSQL — `orchestrator-service`

```sql
users
  id UUID PRIMARY KEY
  email VARCHAR UNIQUE NOT NULL
  password_hash VARCHAR NOT NULL
  role VARCHAR NOT NULL  -- ADMIN | OPERATOR
  created_at TIMESTAMP

jobs
  id UUID PRIMARY KEY
  name VARCHAR NOT NULL
  type VARCHAR NOT NULL          -- HTTP_CALL | CSV_PROCESS | DATA_VALIDATE | REPORT_GENERATE
  config JSONB
  schedule VARCHAR               -- cron expression
  timezone VARCHAR DEFAULT 'UTC' -- np. Europe/Warsaw
  enabled BOOLEAN DEFAULT true
  deleted BOOLEAN DEFAULT false
  deleted_at TIMESTAMP           -- soft delete timestamp
  timeout INT DEFAULT 30         -- max czas wykonania w sekundach
  max_retries INT DEFAULT 3
  notification_channel VARCHAR   -- EMAIL | WEBHOOK | null
  notification_target VARCHAR    -- adres email lub URL webhooka
  created_by UUID REFERENCES users(id)
  created_at TIMESTAMP
  updated_at TIMESTAMP
  last_run_at TIMESTAMP
  last_status VARCHAR            -- SUCCESS | FAILED | RUNNING | PENDING

execution_logs
  id UUID PRIMARY KEY
  job_id UUID REFERENCES jobs(id)
  started_at TIMESTAMP NOT NULL
  finished_at TIMESTAMP
  status VARCHAR NOT NULL        -- SUCCESS | FAILED | RUNNING
  output TEXT
  error_message TEXT
  attempt INT DEFAULT 1
  correlation_id UUID            -- do śledzenia między serwisami

-- Indeksy (wydajność filtrowania i retencji)
CREATE INDEX idx_execution_logs_job_id ON execution_logs(job_id);
CREATE INDEX idx_execution_logs_status ON execution_logs(status);
CREATE INDEX idx_execution_logs_started_at ON execution_logs(started_at);

alerts
  id UUID PRIMARY KEY
  job_id UUID REFERENCES jobs(id)
  triggered_at TIMESTAMP NOT NULL
  reason TEXT
  severity VARCHAR DEFAULT 'ERROR'  -- INFO | WARN | ERROR | CRITICAL
  acknowledged BOOLEAN DEFAULT false
  acknowledged_at TIMESTAMP
  acknowledged_by UUID REFERENCES users(id)
```

### MySQL — `analytics-service`

Dane zasilane przez ETL CronJob (co 1 godzinę z PostgreSQL).

```sql
daily_stats
  id INT AUTO_INCREMENT PRIMARY KEY
  date DATE UNIQUE NOT NULL
  total_runs INT DEFAULT 0
  success_count INT DEFAULT 0
  fail_count INT DEFAULT 0
  avg_duration_ms BIGINT
  calculated_at TIMESTAMP        -- kiedy ETL ostatnio zaktualizował rekord

job_performance
  id INT AUTO_INCREMENT PRIMARY KEY
  job_id VARCHAR(36) NOT NULL
  job_name VARCHAR NOT NULL
  avg_duration_ms BIGINT
  success_rate DECIMAL(5,2)
  total_runs INT
  last_calculated_at TIMESTAMP

CREATE INDEX idx_daily_stats_date ON daily_stats(date);
CREATE INDEX idx_job_performance_job_id ON job_performance(job_id);
```

---

## 10. Harmonogram budowy

| Tydzień | Zakres prac |
|---|---|
| **1** | Setup projektu, Docker Compose, bazy danych (PostgreSQL + MySQL), migracje (Flyway + Alembic), auth JWT z rate limitingiem |
| **2** | CRUD dla Jobs (z timezone, timeout, maxRetries), Scheduler (Quartz), testy jednostkowe |
| **3** | Worker service — executor `HTTP_CALL` + `CSV_PROCESS`; correlationId między serwisami |
| **4** | Execution logs, retry logic, timeout cleanup, alerty z powiadomieniami (email/webhook), `DATA_VALIDATE` + `REPORT_GENERATE` |
| **5** | Analytics service, ETL CronJob (PostgreSQL → MySQL), statystyki, eksport CSV |
| **6** | Kubernetes manifesty (Deployment, CronJob etl-job, HPA, ConfigMap, Secrets, PVC), finalne testy integracyjne, README |
| **7** | CI/CD pipeline (GitHub Actions): build, test, Docker image push, deploy do K8s; dokumentacja Swagger/OpenAPI |

---

## 11. Pokrycie technologii z oferty Craftware

| Technologia z oferty | Zastosowanie w projekcie |
|---|---|
| Java 17 | `orchestrator-service` — cały backend |
| Spring Boot | REST API, DI, Security, JPA, Scheduler |
| Maven | Build `orchestrator-service` (pom.xml) |
| Python | `worker-service` + `analytics-service` |
| FastAPI | REST API dla workera i analityki |
| PostgreSQL | Baza `orchestrator-service` |
| MySQL | Baza `worker-service` / analityki |
| Git | Branching: `main`, `dev`, `feature/*` |
| Docker | Dockerfile dla każdego serwisu |
| Docker Compose | Lokalny dev environment |
| Kubernetes | Deployment, CronJob, HPA, ConfigMap, PVC |
| OOP | Modele, serwisy, repozytoria, wzorce (Strategy dla executorów) |
