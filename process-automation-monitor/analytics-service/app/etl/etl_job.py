"""
ETL job: aggregates execution_logs from PostgreSQL (orchestrator)
into daily_stats and job_performance tables in MySQL (analytics).
Run as K8s CronJob hourly.
"""
import json
import logging
import os
import sys
from datetime import datetime, timezone
from typing import Optional

import psycopg2
import psycopg2.extensions
from sqlalchemy.orm import Session

# Configure structured JSON logging
logging.basicConfig(
    level=logging.INFO,
    format="%(message)s",
    handlers=[logging.StreamHandler(sys.stdout)],
)
logger = logging.getLogger("etl_job")


def _log(level: str, message: str, **kwargs):
    record = {
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "level": level,
        "service": "analytics-etl",
        "message": message,
        **kwargs,
    }
    print(json.dumps(record), flush=True)


DAILY_STATS_QUERY = """
    SELECT
        DATE(started_at) AS date,
        COUNT(*) AS total_runs,
        SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) AS success_count,
        SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) AS fail_count,
        AVG(EXTRACT(EPOCH FROM (finished_at - started_at)) * 1000) AS avg_duration_ms
    FROM execution_logs
    WHERE started_at >= NOW() - INTERVAL '30 days'
    GROUP BY DATE(started_at)
"""

JOB_PERFORMANCE_QUERY = """
    SELECT
        el.job_id,
        j.name AS job_name,
        AVG(EXTRACT(EPOCH FROM (el.finished_at - el.started_at)) * 1000) AS avg_duration_ms,
        SUM(CASE WHEN el.status = 'SUCCESS' THEN 1.0 ELSE 0 END) / COUNT(*) * 100 AS success_rate,
        COUNT(*) AS total_runs
    FROM execution_logs el
    JOIN jobs j ON el.job_id = j.id
    WHERE el.started_at >= NOW() - INTERVAL '30 days'
      AND el.finished_at IS NOT NULL
    GROUP BY el.job_id, j.name
"""

DAILY_UPSERT = """
    INSERT INTO daily_stats (date, total_runs, success_count, fail_count, avg_duration_ms, calculated_at)
    VALUES (%s, %s, %s, %s, %s, %s)
    ON DUPLICATE KEY UPDATE
        total_runs = VALUES(total_runs),
        success_count = VALUES(success_count),
        fail_count = VALUES(fail_count),
        avg_duration_ms = VALUES(avg_duration_ms),
        calculated_at = VALUES(calculated_at)
"""

JOB_PERF_UPSERT = """
    INSERT INTO job_performance (job_id, job_name, avg_duration_ms, success_rate, total_runs, last_calculated_at)
    VALUES (%s, %s, %s, %s, %s, %s)
    ON DUPLICATE KEY UPDATE
        job_name = VALUES(job_name),
        avg_duration_ms = VALUES(avg_duration_ms),
        success_rate = VALUES(success_rate),
        total_runs = VALUES(total_runs),
        last_calculated_at = VALUES(last_calculated_at)
"""


def get_pg_connection() -> psycopg2.extensions.connection:
    """Return a psycopg2 connection from the ORCHESTRATOR_DB_URL env variable."""
    db_url = os.environ.get("ORCHESTRATOR_DB_URL", "")
    if not db_url:
        raise EnvironmentError("ORCHESTRATOR_DB_URL environment variable is not set")
    return psycopg2.connect(db_url)


def get_mysql_session() -> Session:
    """Return a SQLAlchemy session for the analytics MySQL database."""
    # Import here to avoid circular imports when running as __main__
    from app.database import AnalyticsSessionLocal
    return AnalyticsSessionLocal()


def aggregate_daily_stats(
    pg_conn: psycopg2.extensions.connection,
    mysql_session: Session,
) -> int:
    """
    Read daily aggregates from PostgreSQL and upsert into MySQL daily_stats.
    Returns the number of rows processed.
    """
    now = datetime.now(timezone.utc)
    rows_processed = 0

    with pg_conn.cursor() as cursor:
        cursor.execute(DAILY_STATS_QUERY)
        rows = cursor.fetchall()

    if not rows:
        _log("INFO", "aggregate_daily_stats: no rows returned from PostgreSQL")
        return 0

    mysql_conn = mysql_session.connection()
    for row in rows:
        stat_date, total_runs, success_count, fail_count, avg_duration_ms = row
        avg_ms = int(avg_duration_ms) if avg_duration_ms is not None else None
        mysql_conn.execute(
            DAILY_UPSERT,
            (stat_date, int(total_runs), int(success_count), int(fail_count), avg_ms, now),
        )
        rows_processed += 1

    _log("INFO", "aggregate_daily_stats: upserted rows", rows=rows_processed)
    return rows_processed


def aggregate_job_performance(
    pg_conn: psycopg2.extensions.connection,
    mysql_session: Session,
) -> int:
    """
    Read per-job aggregates from PostgreSQL and upsert into MySQL job_performance.
    Returns the number of rows processed.
    """
    now = datetime.now(timezone.utc)
    rows_processed = 0

    with pg_conn.cursor() as cursor:
        cursor.execute(JOB_PERFORMANCE_QUERY)
        rows = cursor.fetchall()

    if not rows:
        _log("INFO", "aggregate_job_performance: no rows returned from PostgreSQL")
        return 0

    mysql_conn = mysql_session.connection()
    for row in rows:
        job_id, job_name, avg_duration_ms, success_rate, total_runs = row
        avg_ms = int(avg_duration_ms) if avg_duration_ms is not None else None
        sr = round(float(success_rate), 2) if success_rate is not None else None
        mysql_conn.execute(
            JOB_PERF_UPSERT,
            (str(job_id), job_name, avg_ms, sr, int(total_runs), now),
        )
        rows_processed += 1

    _log("INFO", "aggregate_job_performance: upserted rows", rows=rows_processed)
    return rows_processed


def run_etl() -> None:
    """Main ETL entry point: connect to both databases, run aggregations, commit, close."""
    _log("INFO", "ETL job started")
    start_time = datetime.now(timezone.utc)

    pg_conn: Optional[psycopg2.extensions.connection] = None
    mysql_session: Optional[Session] = None

    try:
        _log("INFO", "Connecting to PostgreSQL (orchestrator)")
        pg_conn = get_pg_connection()

        _log("INFO", "Connecting to MySQL (analytics)")
        mysql_session = get_mysql_session()

        daily_count = aggregate_daily_stats(pg_conn, mysql_session)
        job_count = aggregate_job_performance(pg_conn, mysql_session)

        mysql_session.commit()

        elapsed = (datetime.now(timezone.utc) - start_time).total_seconds()
        _log(
            "INFO",
            "ETL job completed successfully",
            daily_rows=daily_count,
            job_rows=job_count,
            elapsed_seconds=round(elapsed, 2),
        )

    except EnvironmentError as exc:
        _log("ERROR", "ETL configuration error", error=str(exc))

    except psycopg2.Error as exc:
        _log("ERROR", "PostgreSQL connection/query error", error=str(exc))

    except Exception as exc:
        _log("ERROR", "ETL job failed with unexpected error", error=str(exc))
        if mysql_session:
            try:
                mysql_session.rollback()
            except Exception:
                pass

    finally:
        if pg_conn:
            try:
                pg_conn.close()
            except Exception:
                pass
        if mysql_session:
            try:
                mysql_session.close()
            except Exception:
                pass


if __name__ == "__main__":
    run_etl()
