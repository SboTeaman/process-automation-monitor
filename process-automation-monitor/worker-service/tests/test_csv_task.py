import os
import tempfile
import pandas as pd
import pytest
from app.executor.csv_task import CsvProcessExecutor


def _write_temp_csv(content: str) -> str:
    f = tempfile.NamedTemporaryFile(mode="w", suffix=".csv", delete=False, encoding="utf-8")
    f.write(content)
    f.close()
    return f.name


@pytest.mark.asyncio
async def test_execute_lowercase_transform():
    """Lowercase transform should convert the target column to lowercase."""
    source = _write_temp_csv("email,name\nALICE@Example.COM,Alice\nBOB@TEST.ORG,Bob\n")
    output = source.replace(".csv", "_out.csv")
    try:
        executor = CsvProcessExecutor()
        result = await executor.execute(
            {
                "sourcePath": source,
                "outputPath": output,
                "rules": [{"column": "email", "transform": "lowercase"}],
            }
        )
        assert result["rows_processed"] == 2
        df = pd.read_csv(output)
        assert df["email"].tolist() == ["alice@example.com", "bob@test.org"]
    finally:
        os.unlink(source)
        if os.path.exists(output):
            os.unlink(output)


@pytest.mark.asyncio
async def test_execute_decimal_transform():
    """Type conversion to decimal should produce numeric values."""
    source = _write_temp_csv("name,amount\nAlice,10.5\nBob,20.0\n")
    output = source.replace(".csv", "_out.csv")
    try:
        executor = CsvProcessExecutor()
        result = await executor.execute(
            {
                "sourcePath": source,
                "outputPath": output,
                "rules": [{"column": "amount", "type": "decimal"}],
            }
        )
        assert result["rows_processed"] == 2
        df = pd.read_csv(output)
        assert df["amount"].dtype in (float, "float64")
    finally:
        os.unlink(source)
        if os.path.exists(output):
            os.unlink(output)


@pytest.mark.asyncio
async def test_file_not_found_raises():
    """A missing source file should raise FileNotFoundError."""
    executor = CsvProcessExecutor()
    with pytest.raises(FileNotFoundError, match="Source file not found"):
        await executor.execute(
            {"sourcePath": "/nonexistent/path/data.csv", "outputPath": "/tmp/out.csv"}
        )


@pytest.mark.asyncio
async def test_execute_no_rules_passthrough():
    """When no rules are provided, the file should be copied unchanged."""
    source = _write_temp_csv("a,b\n1,2\n3,4\n")
    output = source.replace(".csv", "_out.csv")
    try:
        executor = CsvProcessExecutor()
        result = await executor.execute(
            {"sourcePath": source, "outputPath": output, "rules": []}
        )
        assert result["rows_processed"] == 2
    finally:
        os.unlink(source)
        if os.path.exists(output):
            os.unlink(output)


@pytest.mark.asyncio
async def test_execute_unknown_column_skipped():
    """Rules targeting non-existent columns should be silently ignored."""
    source = _write_temp_csv("name\nAlice\nBob\n")
    output = source.replace(".csv", "_out.csv")
    try:
        executor = CsvProcessExecutor()
        result = await executor.execute(
            {
                "sourcePath": source,
                "outputPath": output,
                "rules": [{"column": "nonexistent", "transform": "lowercase"}],
            }
        )
        assert result["rows_processed"] == 2
    finally:
        os.unlink(source)
        if os.path.exists(output):
            os.unlink(output)
