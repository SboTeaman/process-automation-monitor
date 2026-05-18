import asyncio
import json
import logging
from datetime import datetime, timezone
from typing import Any

from fastapi import APIRouter, Depends, HTTPException, Request, Response
from sqlalchemy.orm import Session

from app.database import get_db
from app.executor import (
    CsvProcessExecutor,
    DataValidateExecutor,
    HttpCallExecutor,
    ReportGenerateExecutor,
)
from app.executor.base import BaseExecutor
from app.models import JobResult
from app.schemas import ErrorResponse, ExecuteRequest, ExecuteResponse

logger = logging.getLogger(__name__)

router = APIRouter(tags=["execute"])

EXECUTOR_MAP: dict[str, type[BaseExecutor]] = {
    "HTTP_CALL": HttpCallExecutor,
    "CSV_PROCESS": CsvProcessExecutor,
    "DATA_VALIDATE": DataValidateExecutor,
    "REPORT_GENERATE": ReportGenerateExecutor,
}

BACKOFF_SECONDS = [5, 15, 30]


def _get_correlation_id(request: Request) -> str:
    return getattr(request.state, "correlation_id", "")


def _make_error_response(
    error: str,
    code: str,
    correlation_id: str,
    status_code: int = 500,
) -> ErrorResponse:
    return ErrorResponse(
        error=error,
        code=code,
        timestamp=datetime.now(timezone.utc),
        correlationId=correlation_id,
    )


@router.post("/execute", response_model=ExecuteResponse)
async def execute_job(
    body: ExecuteRequest,
    request: Request,
    response: Response,
    db: Session = Depends(get_db),
) -> Any:
    """Accept a job execution request, run the appropriate executor with retry logic,
    persist the result to the database, and return the execution outcome."""

    correlation_id = _get_correlation_id(request)
    if body.correlation_id:
        correlation_id = body.correlation_id

    response.headers["X-Correlation-Id"] = correlation_id

    executor_class = EXECUTOR_MAP.get(body.job_type)
    if executor_class is None:
        raise HTTPException(status_code=422, detail=f"Unknown job_type: {body.job_type}")

    started_at = datetime.now(timezone.utc)

    job_record = JobResult(
        job_id=body.job_id,
        job_type=body.job_type,
        status="RUNNING",
        attempt=1,
        started_at=started_at,
        correlation_id=correlation_id,
    )
    db.add(job_record)
    db.commit()
    db.refresh(job_record)

    executor = executor_class()
    result_output: dict | None = None
    last_error: str | None = None
    attempt = 0

    max_attempts = body.max_retries + 1

    for attempt in range(1, max_attempts + 1):
        job_record.attempt = attempt
        db.commit()

        try:
            result_output = await asyncio.wait_for(
                executor.execute(body.config),
                timeout=body.timeout,
            )
            last_error = None
            break
        except asyncio.TimeoutError:
            last_error = f"Execution timed out after {body.timeout}s (attempt {attempt})"
            logger.warning(
                "Job %s timed out on attempt %d/%d",
                body.job_id,
                attempt,
                max_attempts,
            )
        except Exception as exc:
            last_error = str(exc)
            logger.warning(
                "Job %s failed on attempt %d/%d: %s",
                body.job_id,
                attempt,
                max_attempts,
                last_error,
            )

        if attempt < max_attempts:
            backoff = BACKOFF_SECONDS[min(attempt - 1, len(BACKOFF_SECONDS) - 1)]
            await asyncio.sleep(backoff)

    finished_at = datetime.now(timezone.utc)

    if last_error is None:
        job_record.status = "SUCCESS"
        job_record.output = json.dumps(result_output)
        job_record.error_message = None
    else:
        job_record.status = "FAILED"
        job_record.output = None
        job_record.error_message = last_error

    job_record.finished_at = finished_at
    db.commit()
    db.refresh(job_record)

    return ExecuteResponse(
        job_id=job_record.job_id,
        status=job_record.status,
        output=result_output,
        error_message=job_record.error_message,
        attempt=job_record.attempt,
        started_at=job_record.started_at,
        finished_at=job_record.finished_at,
        correlation_id=job_record.correlation_id,
    )


@router.get("/results/{job_id}/latest", response_model=ExecuteResponse)
async def get_latest_result(
    job_id: str,
    request: Request,
    response: Response,
    db: Session = Depends(get_db),
) -> Any:
    """Return the most recent JobResult for the given job_id."""

    correlation_id = _get_correlation_id(request)
    response.headers["X-Correlation-Id"] = correlation_id

    record = (
        db.query(JobResult)
        .filter(JobResult.job_id == job_id)
        .order_by(JobResult.created_at.desc())
        .first()
    )

    if record is None:
        raise HTTPException(status_code=404, detail=f"No results found for job_id: {job_id}")

    output_dict: dict | None = None
    if record.output:
        try:
            output_dict = json.loads(record.output)
        except (json.JSONDecodeError, TypeError):
            output_dict = {"raw": record.output}

    return ExecuteResponse(
        job_id=record.job_id,
        status=record.status,
        output=output_dict,
        error_message=record.error_message,
        attempt=record.attempt,
        started_at=record.started_at,
        finished_at=record.finished_at,
        correlation_id=record.correlation_id,
    )
