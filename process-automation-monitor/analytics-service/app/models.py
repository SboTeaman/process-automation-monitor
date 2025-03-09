from sqlalchemy import (
    Column, Integer, BigInteger, String, Date, DateTime, Numeric, Index
)
from sqlalchemy.orm import DeclarativeBase
from datetime import datetime


class Base(DeclarativeBase):
    pass


class DailyStat(Base):
    __tablename__ = "daily_stats"

    id = Column(Integer, primary_key=True, autoincrement=True)
    date = Column(Date, unique=True, nullable=False)
    total_runs = Column(Integer, nullable=False, default=0)
    success_count = Column(Integer, nullable=False, default=0)
    fail_count = Column(Integer, nullable=False, default=0)
    avg_duration_ms = Column(BigInteger, nullable=True)
    calculated_at = Column(DateTime, nullable=True, default=datetime.utcnow)

    __table_args__ = (
        Index("idx_daily_stats_date", "date"),
    )

    def __repr__(self) -> str:
        return f"<DailyStat date={self.date} total_runs={self.total_runs}>"


class JobPerformance(Base):
    __tablename__ = "job_performance"

    id = Column(Integer, primary_key=True, autoincrement=True)
    job_id = Column(String(36), nullable=False, index=True)
    job_name = Column(String(255), nullable=False)
    avg_duration_ms = Column(BigInteger, nullable=True)
    success_rate = Column(Numeric(5, 2), nullable=True)
    total_runs = Column(Integer, nullable=False, default=0)
    last_calculated_at = Column(DateTime, nullable=True, default=datetime.utcnow)

    __table_args__ = (
        Index("idx_job_performance_job_id", "job_id"),
    )

    def __repr__(self) -> str:
        return f"<JobPerformance job_id={self.job_id} job_name={self.job_name}>"
