"""
Test configuration: in-memory SQLite engine + FastAPI TestClient fixtures.
"""
import pytest
from datetime import date, datetime
from decimal import Decimal
from unittest.mock import patch

from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.models import Base, DailyStat, JobPerformance

# ---------------------------------------------------------------------------
# SQLite in-memory engine with StaticPool so all connections share one DB
# ---------------------------------------------------------------------------
SQLITE_URL = "sqlite://"

engine = create_engine(
    SQLITE_URL,
    connect_args={"check_same_thread": False},
    poolclass=StaticPool,
)

TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


@pytest.fixture(scope="function")
def db_session():
    """Fresh in-memory schema for every test function."""
    Base.metadata.create_all(bind=engine)
    session = TestingSessionLocal()
    try:
        yield session
    finally:
        session.close()
        Base.metadata.drop_all(bind=engine)


@pytest.fixture(scope="function")
def client(db_session):
    """TestClient with the SQLite DB session injected and MySQL startup bypassed."""
    # Import app here (after engine is set up) to avoid premature MySQL connections
    from app.main import app
    from app.database import get_db

    def override_get_db():
        try:
            yield db_session
        finally:
            pass

    app.dependency_overrides[get_db] = override_get_db

    # Patch the startup event so it doesn't try to connect to MySQL
    with patch("app.main.Base.metadata.create_all"):
        with TestClient(app, raise_server_exceptions=False) as c:
            yield c

    app.dependency_overrides.clear()


# ---------------------------------------------------------------------------
# Sample data helpers
# ---------------------------------------------------------------------------
def make_daily_stat(
    session,
    stat_date: date,
    total_runs: int = 10,
    success_count: int = 8,
    fail_count: int = 2,
    avg_duration_ms: int = 500,
) -> DailyStat:
    stat = DailyStat(
        date=stat_date,
        total_runs=total_runs,
        success_count=success_count,
        fail_count=fail_count,
        avg_duration_ms=avg_duration_ms,
        calculated_at=datetime.utcnow(),
    )
    session.add(stat)
    session.commit()
    session.refresh(stat)
    return stat


def make_job_performance(
    session,
    job_id: str = "job-123",
    job_name: str = "Test Job",
    avg_duration_ms: int = 300,
    success_rate: float = 80.0,
    total_runs: int = 50,
) -> JobPerformance:
    job = JobPerformance(
        job_id=job_id,
        job_name=job_name,
        avg_duration_ms=avg_duration_ms,
        success_rate=Decimal(str(success_rate)),
        total_runs=total_runs,
        last_calculated_at=datetime.utcnow(),
    )
    session.add(job)
    session.commit()
    session.refresh(job)
    return job
