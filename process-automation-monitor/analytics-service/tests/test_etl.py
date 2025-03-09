"""
Tests for the ETL job (etl_job.py).
Uses mocked psycopg2 cursors and SQLAlchemy sessions.
"""
import pytest
from datetime import date, datetime
from decimal import Decimal
from unittest.mock import MagicMock, patch, call

from app.etl.etl_job import (
    aggregate_daily_stats,
    aggregate_job_performance,
    run_etl,
)


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
def _make_pg_conn(rows):
    """Return a mock psycopg2 connection whose cursor yields `rows`."""
    cursor = MagicMock()
    cursor.fetchall.return_value = rows
    cursor.__enter__ = lambda s: s
    cursor.__exit__ = MagicMock(return_value=False)

    conn = MagicMock()
    conn.cursor.return_value = cursor
    return conn, cursor


def _make_mysql_session():
    """Return a mock SQLAlchemy session with a connection that accepts execute."""
    mysql_conn = MagicMock()
    session = MagicMock()
    session.connection.return_value = mysql_conn
    return session, mysql_conn


# ---------------------------------------------------------------------------
# aggregate_daily_stats
# ---------------------------------------------------------------------------
class TestAggregateDailyStats:
    def test_aggregate_daily_stats_inserts_rows(self):
        rows = [
            (date(2024, 1, 1), 10, 8, 2, 500.0),
            (date(2024, 1, 2), 20, 18, 2, 300.0),
        ]
        pg_conn, cursor = _make_pg_conn(rows)
        session, mysql_conn = _make_mysql_session()

        count = aggregate_daily_stats(pg_conn, session)

        assert count == 2
        assert mysql_conn.execute.call_count == 2

    def test_aggregate_daily_stats_handles_null_duration(self):
        rows = [(date(2024, 1, 1), 5, 3, 2, None)]
        pg_conn, _ = _make_pg_conn(rows)
        session, mysql_conn = _make_mysql_session()

        count = aggregate_daily_stats(pg_conn, session)

        assert count == 1
        # Verify the None avg_duration_ms is passed through
        args = mysql_conn.execute.call_args[0][1]
        assert args[4] is None  # avg_duration_ms position

    def test_etl_handles_empty_daily_data(self):
        pg_conn, _ = _make_pg_conn([])
        session, mysql_conn = _make_mysql_session()

        count = aggregate_daily_stats(pg_conn, session)

        assert count == 0
        mysql_conn.execute.assert_not_called()


# ---------------------------------------------------------------------------
# aggregate_job_performance
# ---------------------------------------------------------------------------
class TestAggregateJobPerformance:
    def test_aggregate_job_performance_inserts_rows(self):
        rows = [
            ("job-uuid-1", "Alpha Job", 400.0, 90.0, 100),
            ("job-uuid-2", "Beta Job", 200.0, 75.0, 50),
        ]
        pg_conn, _ = _make_pg_conn(rows)
        session, mysql_conn = _make_mysql_session()

        count = aggregate_job_performance(pg_conn, session)

        assert count == 2
        assert mysql_conn.execute.call_count == 2

    def test_etl_handles_empty_job_performance_data(self):
        pg_conn, _ = _make_pg_conn([])
        session, mysql_conn = _make_mysql_session()

        count = aggregate_job_performance(pg_conn, session)

        assert count == 0
        mysql_conn.execute.assert_not_called()


# ---------------------------------------------------------------------------
# run_etl
# ---------------------------------------------------------------------------
class TestRunEtl:
    def test_run_etl_commits_on_success(self):
        rows_daily = [(date(2024, 1, 1), 5, 4, 1, 100.0)]
        rows_jobs = [("j1", "Job1", 200.0, 80.0, 10)]

        pg_conn = MagicMock()
        pg_cursor = MagicMock()
        pg_cursor.__enter__ = lambda s: s
        pg_cursor.__exit__ = MagicMock(return_value=False)
        pg_cursor.fetchall.side_effect = [rows_daily, rows_jobs]
        pg_conn.cursor.return_value = pg_cursor

        mysql_conn_obj = MagicMock()
        mysql_session = MagicMock()
        mysql_session.connection.return_value = mysql_conn_obj

        with patch("app.etl.etl_job.get_pg_connection", return_value=pg_conn), \
             patch("app.etl.etl_job.get_mysql_session", return_value=mysql_session):
            run_etl()

        mysql_session.commit.assert_called_once()
        mysql_session.close.assert_called_once()
        pg_conn.close.assert_called_once()

    def test_etl_handles_pg_connection_error(self):
        """PostgreSQL connection failure must be caught — not raised."""
        with patch(
            "app.etl.etl_job.get_pg_connection",
            side_effect=Exception("pg connection refused"),
        ), patch("app.etl.etl_job.get_mysql_session") as mock_mysql:
            # Should NOT raise
            run_etl()

        # MySQL session should never have been used
        mock_mysql.assert_not_called()

    def test_etl_handles_environment_error(self):
        """Missing ORCHESTRATOR_DB_URL must be caught gracefully."""
        import os
        original = os.environ.pop("ORCHESTRATOR_DB_URL", None)
        try:
            with patch(
                "app.etl.etl_job.get_pg_connection",
                side_effect=EnvironmentError("ORCHESTRATOR_DB_URL is not set"),
            ):
                run_etl()  # must not raise
        finally:
            if original is not None:
                os.environ["ORCHESTRATOR_DB_URL"] = original
