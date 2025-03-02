import csv
import json
import os
import re
from sqlalchemy import text
from app.config import settings
from app.executor.base import BaseExecutor
from app.database import engine
from app.executor.csv_task import _safe_path

# Blocklist catches the most obvious mutations; the whitelist below is the real guard.
_DISALLOWED_KEYWORDS = re.compile(
    r"\b(INSERT|UPDATE|DELETE|DROP|TRUNCATE|ALTER|CREATE|REPLACE|EXEC|EXECUTE"
    r"|GRANT|REVOKE|LOAD|OUTFILE|DUMPFILE|INTO)\b",
    re.IGNORECASE,
)

# Only tables that belong to this service may be queried.
_ALLOWED_TABLES: frozenset[str] = frozenset({
    "job_results",
})

_TABLE_REF_RE = re.compile(r"\bFROM\s+([a-zA-Z_][a-zA-Z0-9_]*)", re.IGNORECASE)


def _validate_query(query: str) -> None:
    """Raise ValueError if the query is unsafe.

    Two layers:
    1. Keyword blocklist — rejects mutations even inside sub-expressions.
    2. Table whitelist — only _ALLOWED_TABLES may appear after FROM.
    """
    stripped = query.strip()
    if not stripped.upper().startswith("SELECT"):
        raise ValueError("Only SELECT queries are allowed.")
    if _DISALLOWED_KEYWORDS.search(stripped):
        raise ValueError(
            "Query contains forbidden keywords (INSERT, UPDATE, DELETE, DROP…)."
        )
    tables_used = {m.group(1).lower() for m in _TABLE_REF_RE.finditer(stripped)}
    disallowed = tables_used - _ALLOWED_TABLES
    if disallowed:
        raise ValueError(
            f"Query references disallowed table(s): {disallowed}. "
            f"Allowed: {_ALLOWED_TABLES}"
        )


class ReportGenerateExecutor(BaseExecutor):
    """Executor for REPORT_GENERATE tasks — runs a SELECT query and writes output."""

    async def execute(self, config: dict) -> dict:
        """Execute a SQL query and write the result to a file.

        Args:
            config: Must contain 'query', 'format' (JSON|CSV), and 'outputPath'.
                    Optional: 'groupBy' (day|week|month).

        Returns:
            {"rows": int, "format": str, "output_path": str}

        Raises:
            ValueError: If the query is unsafe or outputPath escapes the data directory.
        """
        query: str = config.get("query", "")
        fmt: str = config.get("format", "JSON").upper()
        raw_output: str = config.get("outputPath", "")

        _validate_query(query)
        output_path = _safe_path(raw_output, settings.DATA_DIR)

        with engine.connect() as conn:
            result = conn.execute(text(query))
            columns = list(result.keys())
            rows = [dict(zip(columns, row)) for row in result.fetchall()]

        output_dir = os.path.dirname(output_path)
        if output_dir:
            os.makedirs(output_dir, exist_ok=True)

        if fmt == "JSON":
            with open(output_path, "w", encoding="utf-8") as f:
                json.dump(rows, f, indent=2, default=str)
        elif fmt == "CSV":
            with open(output_path, "w", newline="", encoding="utf-8") as f:
                writer = csv.DictWriter(f, fieldnames=columns)
                writer.writeheader()
                writer.writerows(rows)
        else:
            raise ValueError(f"Unsupported format '{fmt}'. Use 'JSON' or 'CSV'.")

        return {"rows": len(rows), "format": fmt, "output_path": raw_output}
