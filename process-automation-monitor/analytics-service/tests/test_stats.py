"""
Tests for /stats/* and /reports/export endpoints.
"""
import pytest
from datetime import date, timedelta

from tests.conftest import make_daily_stat, make_job_performance


class TestSummary:
    def test_summary_returns_zeros_when_no_data(self, client):
        response = client.get("/stats/summary")
        assert response.status_code == 200
        body = response.json()
        assert body["total_runs_24h"] == 0
        assert body["success_rate_24h"] == 0.0
        assert body["error_count_24h"] == 0
        assert body["total_jobs"] == 0

    def test_summary_with_data(self, client, db_session):
        today = date.today()
        make_daily_stat(
            db_session,
            stat_date=today,
            total_runs=20,
            success_count=16,
            fail_count=4,
        )
        response = client.get("/stats/summary")
        assert response.status_code == 200
        body = response.json()
        assert body["total_runs_24h"] == 20
        assert body["error_count_24h"] == 4
        assert body["success_rate_24h"] == pytest.approx(80.0, abs=0.1)

    def test_summary_success_rate_100_percent(self, client, db_session):
        make_daily_stat(
            db_session,
            stat_date=date.today(),
            total_runs=10,
            success_count=10,
            fail_count=0,
        )
        response = client.get("/stats/summary")
        assert response.status_code == 200
        assert response.json()["success_rate_24h"] == pytest.approx(100.0)


class TestDailyStats:
    def test_daily_stats_empty(self, client):
        response = client.get("/stats/daily")
        assert response.status_code == 200
        body = response.json()
        assert body["days"] == []
        assert body["period_days"] == 30

    def test_daily_stats_30_days(self, client, db_session):
        today = date.today()
        for i in range(30):
            make_daily_stat(db_session, stat_date=today - timedelta(days=i))

        response = client.get("/stats/daily")
        assert response.status_code == 200
        body = response.json()
        assert len(body["days"]) == 30
        assert body["period_days"] == 30

    def test_daily_stats_ordered_asc(self, client, db_session):
        today = date.today()
        make_daily_stat(db_session, stat_date=today - timedelta(days=2))
        make_daily_stat(db_session, stat_date=today - timedelta(days=1))
        make_daily_stat(db_session, stat_date=today)

        response = client.get("/stats/daily")
        days = response.json()["days"]
        dates = [d["date"] for d in days]
        assert dates == sorted(dates)


class TestJobPerformance:
    def test_job_performance_found(self, client, db_session):
        make_job_performance(
            db_session,
            job_id="abc-123",
            job_name="My Job",
            avg_duration_ms=400,
            success_rate=90.0,
            total_runs=100,
        )
        response = client.get("/stats/jobs/abc-123/performance")
        assert response.status_code == 200
        body = response.json()
        assert body["job_id"] == "abc-123"
        assert body["job_name"] == "My Job"
        assert body["avg_duration_ms"] == 400
        assert body["success_rate"] == pytest.approx(90.0)
        assert body["total_runs"] == 100

    def test_job_performance_not_found(self, client):
        response = client.get("/stats/jobs/nonexistent-id/performance")
        assert response.status_code == 404
        body = response.json()
        # FastAPI wraps HTTPException detail in {"detail": ...}
        assert "detail" in body


class TestTopFailing:
    def test_top_failing_empty(self, client):
        response = client.get("/stats/top-failing")
        assert response.status_code == 200
        assert response.json() == []

    def test_top_failing_returns_sorted_by_failure_rate(self, client, db_session):
        make_job_performance(
            db_session, job_id="j1", job_name="Job1", success_rate=90.0, total_runs=100
        )
        make_job_performance(
            db_session, job_id="j2", job_name="Job2", success_rate=50.0, total_runs=100
        )
        make_job_performance(
            db_session, job_id="j3", job_name="Job3", success_rate=70.0, total_runs=100
        )

        response = client.get("/stats/top-failing")
        assert response.status_code == 200
        items = response.json()
        # j2 has highest failure rate (50%), should be first
        assert items[0]["job_id"] == "j2"

    def test_top_failing_max_10_results(self, client, db_session):
        for i in range(15):
            make_job_performance(
                db_session,
                job_id=f"job-{i}",
                job_name=f"Job {i}",
                success_rate=50.0,
                total_runs=10,
            )
        response = client.get("/stats/top-failing")
        assert len(response.json()) <= 10


class TestExportCSV:
    def test_export_csv_content_type(self, client):
        response = client.get("/reports/export")
        assert response.status_code == 200
        assert "text/csv" in response.headers["content-type"]

    def test_export_csv_headers(self, client, db_session):
        make_daily_stat(db_session, stat_date=date.today())
        response = client.get("/reports/export")
        assert response.status_code == 200
        lines = response.text.strip().split("\n")
        header = lines[0]
        assert "date" in header
        assert "total_runs" in header
        assert "success_count" in header
        assert "fail_count" in header
        assert "avg_duration_ms" in header

    def test_export_csv_data_rows(self, client, db_session):
        make_daily_stat(
            db_session,
            stat_date=date.today(),
            total_runs=5,
            success_count=4,
            fail_count=1,
        )
        response = client.get("/reports/export")
        lines = response.text.strip().split("\n")
        # Header + 1 data row
        assert len(lines) == 2
        assert "5" in lines[1]
