from datetime import datetime
from typing import Literal, Optional
from pydantic import BaseModel, Field


class ExecuteRequest(BaseModel):
    job_id: str
    job_type: Literal["HTTP_CALL", "CSV_PROCESS", "DATA_VALIDATE", "REPORT_GENERATE"]
    config: dict
    timeout: int = Field(default=30, ge=1, le=300)
    max_retries: int = Field(default=3, ge=0, le=10)
    correlation_id: Optional[str] = None


class ExecuteResponse(BaseModel):
    job_id: str
    status: str
    output: Optional[dict] = None
    error_message: Optional[str] = None
    attempt: int
    started_at: datetime
    finished_at: Optional[datetime] = None
    correlation_id: Optional[str] = None

    model_config = {"from_attributes": True}


class HealthResponse(BaseModel):
    status: str
    service: str
    timestamp: datetime


class ErrorResponse(BaseModel):
    error: str
    code: str
    timestamp: datetime
    correlationId: Optional[str] = None
