import json
import pytest
from datetime import datetime, timezone
from unittest.mock import AsyncMock, MagicMock, patch

from fastapi.testclient import TestClient


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _make_job_record(
    status: str = "SUCCESS",
    attempt: int = 1,
    error_message: str | None = None,
    output: str | None = None,
):
    record = MagicMock()
    record.job_id = "test-job-123"
    record.job_type = "HTTP_CALL"
    record.status = status
    record.attempt = attempt
    record.error_message = error_message
    record.output = output or json.dumps({"status_code": 200, "body": "ok", "matched": True})
    record.started_at = datetime.now(timezone.utc)
    record.finished_at = datetime.now(timezone.utc)
    record.correlation_id = "corr-id-abc"
    return record


def _build_mock_session(record=None):
    session = MagicMock()
    session.add = MagicMock()
    session.commit = MagicMock()
    session.refresh = MagicMock()
    session.query.return_value.filter.return_value.order_by.return_value.first.return_value = record
    return session


def _get_app():
    """Import and return the FastAPI app, patching the DB engine so no real
    connection is attempted at startup."""
    mock_engine = MagicMock()
    mock_engine.connect.return_value.__enter__ = MagicMock(return_value=mock_engine)
    mock_engine.connect.return_value.__exit__ = MagicMock(return_value=False)
    with patch("app.database.engine", mock_engine):
        from app.main import app  # noqa: PLC0415
        return app


# ---------------------------------------------------------------------------
# Tests
# ---------------------------------------------------------------------------

def test_http_call_success():
    """A successful HTTP_CALL job should return 200 with SUCCESS status."""
    from app.database import get_db

    mock_session = _build_mock_session()
    mock_executor = AsyncMock()
    mock_executor.execute.return_value = {"status_code": 200, "body": "ok", "matched": True}

    app = _get_app()
    app.dependency_overrides[get_db] = lambda: mock_session

    try:
        with patch("app.routes.execute.HttpCallExecutor", return_value=mock_executor):
            with TestClient(app, raise_server_exceptions=False) as client:
                response = client.post(
                    "/execute",
                    json={
                        "job_id": "test-job-123",
                        "job_type": "HTTP_CALL",
                        "config": {"url": "https://example.com", "method": "GET"},
                        "timeout": 30,
                        "max_retries": 0,
                    },
                )
    finally:
        app.dependency_overrides.clear()

    assert response.status_code == 200
    data = response.json()
    assert data["job_id"] == "test-job-123"


def test_invalid_job_type_returns_422():
    """An unknown job_type should cause FastAPI to return 422."""
    app = _get_app()
    with TestClient(app, raise_server_exceptions=False) as client:
        response = client.post(
            "/execute",
            json={"job_id": "bad-job", "job_type": "INVALID_TYPE", "config": {}},
        )
    assert response.status_code == 422


def test_missing_job_id_returns_422():
    """A request without job_id should return 422."""
    app = _get_app()
    with TestClient(app, raise_server_exceptions=False) as client:
        response = client.post("/execute", json={"job_type": "HTTP_CALL", "config": {}})
    assert response.status_code == 422


def test_retry_on_failure_increments_attempt():
    """When the executor raises on the first attempt and succeeds on retry, call count == 2."""
    from app.database import get_db
    import app.routes.execute as execute_module

    call_count = 0

    async def _flaky_execute(config):
        nonlocal call_count
        call_count += 1
        if call_count == 1:
            raise RuntimeError("transient error")
        return {"status_code": 200, "body": "ok", "matched": True}

    mock_session = _build_mock_session()

    # Create a fake executor instance
    fake_instance = MagicMock()
    fake_instance.execute = _flaky_execute

    # Patch EXECUTOR_MAP so HTTP_CALL uses our fake executor class
    FakeExecutorClass = MagicMock(return_value=fake_instance)
    patched_map = dict(execute_module.EXECUTOR_MAP)
    patched_map["HTTP_CALL"] = FakeExecutorClass

    app = _get_app()
    app.dependency_overrides[get_db] = lambda: mock_session

    try:
        with (
            patch.object(execute_module, "EXECUTOR_MAP", patched_map),
            patch("asyncio.sleep", new_callable=AsyncMock),
        ):
            with TestClient(app, raise_server_exceptions=False) as client:
                client.post(
                    "/execute",
                    json={
                        "job_id": "retry-job",
                        "job_type": "HTTP_CALL",
                        "config": {"url": "https://example.com", "method": "GET"},
                        "timeout": 30,
                        "max_retries": 3,
                    },
                )
    finally:
        app.dependency_overrides.clear()

    assert call_count == 2


def test_health_endpoint_returns_up():
    """GET /health should return status UP."""
    app = _get_app()
    with TestClient(app, raise_server_exceptions=False) as client:
        response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "UP"
    assert data["service"] == "worker-service"


def test_results_not_found_returns_404():
    """GET /results/{job_id}/latest for an unknown job_id should return 404."""
    from app.database import get_db

    mock_session = _build_mock_session(record=None)

    app = _get_app()
    app.dependency_overrides[get_db] = lambda: mock_session

    try:
        with TestClient(app, raise_server_exceptions=False) as client:
            response = client.get("/results/nonexistent-job/latest")
    finally:
        app.dependency_overrides.clear()

    assert response.status_code == 404


def test_correlation_id_in_response_headers():
    """Response should include an X-Correlation-Id header."""
    app = _get_app()
    with TestClient(app, raise_server_exceptions=False) as client:
        response = client.get("/health")
    assert "x-correlation-id" in {k.lower() for k in response.headers.keys()}
