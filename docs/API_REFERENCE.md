# API Reference

## Base URL

```
http://localhost:8080  (Development)
https://api.example.com (Production)
```

## Authentication

All endpoints require authentication via JWT token in the `Authorization` header:

```
Authorization: Bearer YOUR_JWT_TOKEN
```

## Error Responses

All errors return JSON with status code and message:

```json
{
  "status": 400,
  "message": "Invalid request",
  "error": "validation_error",
  "details": {
    "field": "error message"
  }
}
```

**Common Status Codes**:
- `200` - Success
- `201` - Created
- `400` - Bad request
- `401` - Unauthorized
- `403` - Forbidden
- `404` - Not found
- `500` - Server error

## Authentication Endpoints

### Register User

```http
POST /auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "secure_password",
  "fullName": "User Name"
}
```

**Response** (201):
```json
{
  "id": "user-123",
  "email": "user@example.com",
  "fullName": "User Name",
  "createdAt": "2026-05-17T12:00:00Z"
}
```

### Login

```http
POST /auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "secure_password"
}
```

**Response** (200):
```json
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "user": {
    "id": "user-123",
    "email": "user@example.com"
  }
}
```

### Refresh Token

```http
POST /auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGc..."
}
```

**Response** (200):
```json
{
  "accessToken": "eyJhbGc..."
}
```

## Job Endpoints

### Get All Jobs

```http
GET /jobs?page=0&size=10
Authorization: Bearer TOKEN
```

**Query Parameters**:
- `page` - Page number (0-indexed)
- `size` - Items per page (default: 10)

**Response** (200):
```json
{
  "content": [
    {
      "id": "job-123",
      "name": "Daily Report",
      "type": "CSV_PROCESS",
      "cronExpression": "0 9 * * *",
      "enabled": true,
      "createdAt": "2026-05-17T12:00:00Z",
      "updatedAt": "2026-05-17T12:00:00Z"
    }
  ],
  "totalPages": 5,
  "totalElements": 45,
  "currentPage": 0
}
```

### Get Single Job

```http
GET /jobs/{jobId}
Authorization: Bearer TOKEN
```

**Response** (200):
```json
{
  "id": "job-123",
  "name": "Daily Report",
  "description": "Process daily sales",
  "type": "CSV_PROCESS",
  "cronExpression": "0 9 * * *",
  "timeout": 30,
  "maxRetries": 3,
  "enabled": true,
  "config": {
    "source": {
      "type": "file",
      "path": "/data/sales.csv"
    },
    "processing": {
      "filter": {
        "field": "status",
        "value": "completed"
      },
      "columns": ["date", "amount"],
      "sort": {
        "field": "amount",
        "direction": "desc"
      }
    },
    "output": {
      "format": "csv",
      "path": "/reports/sales.csv"
    }
  },
  "createdAt": "2026-05-17T12:00:00Z",
  "updatedAt": "2026-05-17T12:00:00Z"
}
```

### Create Job

```http
POST /jobs
Authorization: Bearer TOKEN
Content-Type: application/json

{
  "name": "Daily Report",
  "description": "Process daily sales",
  "type": "CSV_PROCESS",
  "cronExpression": "0 9 * * *",
  "timeout": 30,
  "maxRetries": 3,
  "enabled": true,
  "config": {
    "source": {
      "type": "file",
      "path": "/data/sales.csv"
    },
    "processing": {
      "columns": ["date", "amount"]
    },
    "output": {
      "format": "csv",
      "path": "/reports/sales.csv"
    }
  }
}
```

**Response** (201):
```json
{
  "id": "job-123",
  "name": "Daily Report",
  "createdAt": "2026-05-17T12:00:00Z"
}
```

### Update Job

```http
PUT /jobs/{jobId}
Authorization: Bearer TOKEN
Content-Type: application/json

{
  "name": "Updated Daily Report",
  "cronExpression": "0 10 * * *",
  "config": { ... }
}
```

**Response** (200):
```json
{
  "id": "job-123",
  "name": "Updated Daily Report",
  "updatedAt": "2026-05-17T13:00:00Z"
}
```

### Delete Job

```http
DELETE /jobs/{jobId}
Authorization: Bearer TOKEN
```

**Response** (204): No content

### Execute Job Now

```http
POST /jobs/{jobId}/execute
Authorization: Bearer TOKEN
```

**Response** (200):
```json
{
  "executionId": "exec-456",
  "jobId": "job-123",
  "startTime": "2026-05-17T13:00:00Z",
  "status": "RUNNING"
}
```

### Toggle Job Enable/Disable

```http
PATCH /jobs/{jobId}/toggle
Authorization: Bearer TOKEN
```

**Response** (200):
```json
{
  "id": "job-123",
  "enabled": false
}
```

## Execution Endpoints

### Get All Executions

```http
GET /executions?page=0&size=10&jobId=job-123
Authorization: Bearer TOKEN
```

**Query Parameters**:
- `page` - Page number
- `size` - Items per page
- `jobId` - Filter by job (optional)
- `status` - SUCCESS or FAILED (optional)

**Response** (200):
```json
{
  "content": [
    {
      "id": "exec-456",
      "jobId": "job-123",
      "jobName": "Daily Report",
      "startTime": "2026-05-17T09:00:00Z",
      "endTime": "2026-05-17T09:02:30Z",
      "status": "SUCCESS",
      "duration": "2m 30s",
      "errorMessage": null
    }
  ],
  "totalPages": 1,
  "totalElements": 10,
  "currentPage": 0
}
```

### Get Execution Details

```http
GET /executions/{executionId}
Authorization: Bearer TOKEN
```

**Response** (200):
```json
{
  "id": "exec-456",
  "jobId": "job-123",
  "jobName": "Daily Report",
  "startTime": "2026-05-17T09:00:00Z",
  "endTime": "2026-05-17T09:02:30Z",
  "status": "SUCCESS",
  "duration": "2m 30s",
  "logs": "[09:00:00] Starting job\n[09:00:05] Processing file...\n[09:02:30] Completed",
  "errorMessage": null,
  "outputPath": "/reports/sales.csv"
}
```

## Statistics Endpoints

### Dashboard Summary

```http
GET /stats/summary
Authorization: Bearer TOKEN
```

**Response** (200):
```json
{
  "totalJobs": 15,
  "successRate": 98.5,
  "recentErrors": 2,
  "activeAlerts": 1
}
```

### Daily Statistics

```http
GET /stats/daily?days=30
Authorization: Bearer TOKEN
```

**Query Parameters**:
- `days` - Number of days to include (default: 30)

**Response** (200):
```json
[
  {
    "date": "2026-05-17",
    "executed": 12,
    "successful": 12,
    "failed": 0,
    "avgDuration": "2m 15s"
  },
  ...
]
```

### Top Failing Jobs

```http
GET /stats/top-failing?limit=5
Authorization: Bearer TOKEN
```

**Response** (200):
```json
[
  {
    "jobId": "job-123",
    "jobName": "Daily Report",
    "failureCount": 5,
    "lastFailure": "2026-05-17T09:00:00Z"
  }
]
```

### Job Performance

```http
GET /stats/jobs/performance
Authorization: Bearer TOKEN
```

**Response** (200):
```json
[
  {
    "jobId": "job-123",
    "jobName": "Daily Report",
    "avgDuration": "2m 30s",
    "minDuration": "2m 00s",
    "maxDuration": "3m 45s",
    "executionCount": 30,
    "successRate": 100
  }
]
```

## Alert Endpoints

### Get All Alerts

```http
GET /alerts
Authorization: Bearer TOKEN
```

**Response** (200):
```json
[
  {
    "id": "alert-789",
    "name": "Job Failure Alert",
    "jobId": "job-123",
    "triggerType": "JOB_FAILURE",
    "notificationMethod": "EMAIL",
    "recipients": ["admin@example.com"],
    "enabled": true,
    "createdAt": "2026-05-17T12:00:00Z"
  }
]
```

### Create Alert

```http
POST /alerts
Authorization: Bearer TOKEN
Content-Type: application/json

{
  "name": "Job Failure Alert",
  "jobId": "job-123",
  "triggerType": "JOB_FAILURE",
  "notificationMethod": "EMAIL",
  "recipients": ["admin@example.com"],
  "enabled": true
}
```

**Response** (201):
```json
{
  "id": "alert-789",
  "name": "Job Failure Alert",
  "createdAt": "2026-05-17T12:00:00Z"
}
```

### Update Alert

```http
PUT /alerts/{alertId}
Authorization: Bearer TOKEN
```

### Delete Alert

```http
DELETE /alerts/{alertId}
Authorization: Bearer TOKEN
```

**Response** (204): No content

## Rate Limiting

API requests are rate limited:
- **Limit**: 1000 requests per hour
- **Header**: `X-RateLimit-Remaining`

## Pagination

List endpoints use standard pagination:

```json
{
  "content": [...],
  "totalPages": 5,
  "totalElements": 45,
  "currentPage": 0,
  "pageSize": 10
}
```

Navigate with:
- `page` - 0-indexed page number
- `size` - Items per page (1-100)

## Testing API

### Using cURL

```bash
# Get jobs
curl -X GET http://localhost:8080/jobs \
  -H "Authorization: Bearer YOUR_TOKEN"

# Create job
curl -X POST http://localhost:8080/jobs \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "Test", ...}'
```

### Using Postman

1. Import collection from `/docs/postman-collection.json`
2. Set `base_url` variable to `http://localhost:8080`
3. Set `token` variable after login
4. Run requests

## API Versioning

Current API version: **v1**

Future versions will be available at:
- `http://localhost:8080/api/v2/`
- `http://localhost:8080/api/v3/`

## Webhooks (Future)

Planned webhook support for:
- Job execution started
- Job execution completed
- Job execution failed
- Alert triggered

See GitHub issues for status.
