from pydantic import BaseModel
from typing import Optional


class SummaryStats(BaseModel):
    total_jobs: int
    success_rate_24h: float
    error_count_24h: int
    total_runs_24h: int


class DailyStatsEntry(BaseModel):
    date: str
    total_runs: int
    success_count: int
    fail_count: int
    avg_duration_ms: Optional[int] = None


class DailyStatsResponse(BaseModel):
    days: list[DailyStatsEntry]
    period_days: int


class JobPerformanceResponse(BaseModel):
    job_id: str
    job_name: str
    avg_duration_ms: Optional[int] = None
    success_rate: Optional[float] = None
    total_runs: int


class TopFailingJob(BaseModel):
    job_id: str
    job_name: str
    fail_count: int
    total_runs: int
    failure_rate: float


class HealthResponse(BaseModel):
    status: str
    service: str
    timestamp: str


class ErrorResponse(BaseModel):
    error: str
    code: str
    timestamp: str
    correlationId: str
