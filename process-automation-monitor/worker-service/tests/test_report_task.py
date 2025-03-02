import json
import os
import tempfile
import pytest
from unittest.mock import MagicMock, patch
from app.executor.report_task import ReportGenerateExecutor, _is_select_only


def test_non_select_query_is_rejected():
    """INSERT query should be identified as unsafe."""
    assert _is_select_only("INSERT INTO foo VALUES (1)") is False


def test_drop_query_is_rejected():
    """DROP TABLE should be identified as unsafe even when embedded in SELECT."""
    assert _is_select_only("SELECT 1; DROP TABLE users") is False


def test_select_query_is_allowed():
    """A plain SELECT query should be allowed."""
    assert _is_select_only("SELECT id, name FROM jobs WHERE enabled = 1") is True


def test_update_inside_select_is_rejected():
    """UPDATE keyword inside a query string should be rejected."""
    assert _is_select_only("SELECT * FROM t WHERE UPDATE = 1") is False


@pytest.mark.asyncio
async def test_non_select_query_raises_value_error():
    """execute() should raise ValueError for any non-SELECT query."""
    executor = ReportGenerateExecutor()
    with pytest.raises(ValueError, match="SELECT"):
        await executor.execute(
            {
                "query": "DELETE FROM job_results",
                "format": "JSON",
                "outputPath": "/tmp/report.json",
            }
        )


@pytest.mark.asyncio
async def test_select_query_json_output():
    """A valid SELECT query should produce a JSON file."""
    mock_conn = MagicMock()
    mock_result = MagicMock()
    mock_result.keys.return_value = ["id", "name"]
    mock_result.fetchall.return_value = [(1, "Job A"), (2, "Job B")]
    mock_conn.execute.return_value = mock_result
    mock_conn.__enter__ = MagicMock(return_value=mock_conn)
    mock_conn.__exit__ = MagicMock(return_value=False)

    mock_engine = MagicMock()
    mock_engine.connect.return_value = mock_conn

    with tempfile.NamedTemporaryFile(suffix=".json", delete=False) as f:
        output_path = f.name

    try:
        with patch("app.executor.report_task.engine", mock_engine):
            executor = ReportGenerateExecutor()
            result = await executor.execute(
                {
                    "query": "SELECT id, name FROM jobs",
                    "format": "JSON",
                    "outputPath": output_path,
                }
            )
        assert result["rows"] == 2
        assert result["format"] == "JSON"
        assert result["output_path"] == output_path
        with open(output_path, "r") as f:
            data = json.load(f)
        assert len(data) == 2
        assert data[0]["name"] == "Job A"
    finally:
        os.unlink(output_path)


@pytest.mark.asyncio
async def test_select_query_csv_output():
    """A valid SELECT query should produce a CSV file when format is CSV."""
    mock_conn = MagicMock()
    mock_result = MagicMock()
    mock_result.keys.return_value = ["id", "status"]
    mock_result.fetchall.return_value = [(1, "SUCCESS"), (2, "FAILED")]
    mock_conn.execute.return_value = mock_result
    mock_conn.__enter__ = MagicMock(return_value=mock_conn)
    mock_conn.__exit__ = MagicMock(return_value=False)

    mock_engine = MagicMock()
    mock_engine.connect.return_value = mock_conn

    with tempfile.NamedTemporaryFile(suffix=".csv", delete=False) as f:
        output_path = f.name

    try:
        with patch("app.executor.report_task.engine", mock_engine):
            executor = ReportGenerateExecutor()
            result = await executor.execute(
                {
                    "query": "SELECT id, status FROM job_results",
                    "format": "CSV",
                    "outputPath": output_path,
                }
            )
        assert result["rows"] == 2
        assert result["format"] == "CSV"
        with open(output_path, "r") as f:
            content = f.read()
        assert "id,status" in content
        assert "SUCCESS" in content
    finally:
        os.unlink(output_path)
