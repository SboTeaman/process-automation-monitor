import csv
import io
import logging
from datetime import date, datetime, timedelta, timezone
from typing import List

from fastapi import APIRouter, Depends, HTTPException, Request
from fastapi.responses import StreamingResponse
from sqlalchemy.orm import Session
from sqlalchemy import desc

from app.database import get_db
from app.models import DailyStat, JobPerformance
from app.schemas import (
    DailyStatsEntry,
    DailyStatsResponse,
    JobPerformanceResponse,
    SummaryStats,
    TopFailingJob,
)

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/stats", tags=["Statistics"])
reports_router = APIRouter(prefix="/reports", tags=["Reports"])


def _get_correlation_id(request: Request) -> str:
    return request.state.correlation_id


@router.get("/summary", response_model=SummaryStats)
def get_summary(request: Request, db: Session = Depends(get_db)):
    correlation_id = _get_correlation_id(request)
    logger.info("Fetching summary stats", extra={"correlationId": correlation_id})

    yesterday = date.today() - timedelta(days=1)
    today = date.today()

    rows = (
        db.query(DailyStat)
        .filter(DailyStat.date >= yesterday, DailyStat.date <= today)
        .all()
    )

    if not rows:
        return SummaryStats(
            total_jobs=0,
            success_rate_24h=0.0,
            error_count_24h=0,
            total_runs_24h=0,
        )

    total_runs = sum(r.total_runs for r in rows)
    success_count = sum(r.success_count for r in rows)
    fail_count = sum(r.fail_count for r in rows)

    success_rate = (success_count / total_runs * 100) if total_runs > 0 else 0.0

    total_jobs = db.query(JobPerformance).count()

    return SummaryStats(
        total_jobs=total_jobs,
        success_rate_24h=round(success_rate, 2),
        error_count_24h=fail_count,
        total_runs_24h=total_runs,
    )


@router.get("/daily", response_model=DailyStatsResponse)
def get_daily_stats(request: Request, db: Session = Depends(get_db)):
    correlation_id = _get_correlation_id(request)
    logger.info("Fetching daily stats", extra={"correlationId": correlation_id})

    cutoff = date.today() - timedelta(days=30)
    rows = (
        db.query(DailyStat)
        .filter(DailyStat.date >= cutoff)
        .order_by(DailyStat.date.asc())
        .all()
    )

    entries = [
        DailyStatsEntry(
            date=str(r.date),
            total_runs=r.total_runs,
            success_count=r.success_count,
            fail_count=r.fail_count,
            avg_duration_ms=int(r.avg_duration_ms) if r.avg_duration_ms is not None else None,
        )
        for r in rows
    ]

    return DailyStatsResponse(days=entries, period_days=30)


@router.get("/jobs/{job_id}/performance", response_model=JobPerformanceResponse)
def get_job_performance(job_id: str, request: Request, db: Session = Depends(get_db)):
    correlation_id = _get_correlation_id(request)
    logger.info(
        "Fetching job performance",
        extra={"correlationId": correlation_id, "jobId": job_id},
    )

    record = (
        db.query(JobPerformance)
        .filter(JobPerformance.job_id == job_id)
        .first()
    )

    if not record:
        raise HTTPException(
            status_code=404,
            detail={
                "error": f"Job performance data not found for job_id={job_id}",
                "code": "JOB_PERFORMANCE_NOT_FOUND",
                "timestamp": datetime.now(timezone.utc).isoformat(),
                "correlationId": correlation_id,
            },
        )

    return JobPerformanceResponse(
        job_id=record.job_id,
        job_name=record.job_name,
        avg_duration_ms=int(record.avg_duration_ms) if record.avg_duration_ms is not None else None,
        success_rate=float(record.success_rate) if record.success_rate is not None else None,
        total_runs=record.total_runs,
    )


@router.get("/top-failing", response_model=List[TopFailingJob])
def get_top_failing(request: Request, db: Session = Depends(get_db)):
    correlation_id = _get_correlation_id(request)
    logger.info("Fetching top failing jobs", extra={"correlationId": correlation_id})

    rows = (
        db.query(JobPerformance)
        .filter(JobPerformance.total_runs > 0)
        .all()
    )

    results = []
    for r in rows:
        success_rate = float(r.success_rate) if r.success_rate is not None else 0.0
        failure_rate = 100.0 - success_rate
        fail_count = round(r.total_runs * (failure_rate / 100))
        results.append(
            TopFailingJob(
                job_id=r.job_id,
                job_name=r.job_name,
                fail_count=fail_count,
                total_runs=r.total_runs,
                failure_rate=round(failure_rate, 2),
            )
        )

    results.sort(key=lambda x: x.failure_rate * x.total_runs, reverse=True)
    return results[:10]


@reports_router.get("/export")
def export_csv(request: Request, db: Session = Depends(get_db)):
    correlation_id = _get_correlation_id(request)
    logger.info("Exporting CSV report", extra={"correlationId": correlation_id})

    cutoff = date.today() - timedelta(days=30)
    rows = (
        db.query(DailyStat)
        .filter(DailyStat.date >= cutoff)
        .order_by(DailyStat.date.asc())
        .all()
    )

    output = io.StringIO()
    writer = csv.writer(output)
    writer.writerow(["date", "total_runs", "success_count", "fail_count", "avg_duration_ms"])

    for r in rows:
        writer.writerow([
            str(r.date),
            r.total_runs,
            r.success_count,
            r.fail_count,
            int(r.avg_duration_ms) if r.avg_duration_ms is not None else "",
        ])

    output.seek(0)

    return StreamingResponse(
        iter([output.getvalue()]),
        media_type="text/csv",
        headers={"Content-Disposition": "attachment; filename=daily_stats.csv"},
    )
