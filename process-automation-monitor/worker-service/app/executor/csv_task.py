import os
import pandas as pd
from app.config import settings
from app.executor.base import BaseExecutor


def _safe_path(raw_path: str, base_dir: str) -> str:
    """Resolve raw_path and verify it stays within base_dir.

    Raises ValueError on path-traversal attempts (e.g. ../../etc/passwd).
    """
    resolved = os.path.realpath(os.path.abspath(raw_path))
    base = os.path.realpath(os.path.abspath(base_dir))
    if not resolved.startswith(base + os.sep) and resolved != base:
        raise ValueError(
            f"Path '{raw_path}' escapes the allowed directory '{base_dir}'"
        )
    return resolved


class CsvProcessExecutor(BaseExecutor):
    """Executor for CSV_PROCESS tasks — reads, transforms, and writes CSV files."""

    async def execute(self, config: dict) -> dict:
        """Process a CSV file applying transformation rules.

        Args:
            config: Must contain 'sourcePath' and 'outputPath'. Optional: 'delimiter',
                    'encoding', 'rules' (list of transform/type operations).

        Returns:
            {"rows_processed": int, "output_path": str}

        Raises:
            FileNotFoundError: If sourcePath does not exist.
            ValueError: If paths escape the allowed data directory or a rule is invalid.
        """
        raw_source: str = config.get("sourcePath", "")
        delimiter: str = config.get("delimiter", ",")
        encoding: str = config.get("encoding", "UTF-8")
        rules: list = config.get("rules", [])
        raw_output: str = config.get("outputPath", "")

        source_path = _safe_path(raw_source, settings.DATA_DIR)
        output_path = _safe_path(raw_output, settings.DATA_DIR)

        if not os.path.exists(source_path):
            raise FileNotFoundError(f"Source file not found: {source_path}")

        df = pd.read_csv(source_path, delimiter=delimiter, encoding=encoding)

        for rule in rules:
            column = rule.get("column")
            if column not in df.columns:
                continue

            transform = rule.get("transform")
            col_type = rule.get("type")

            if transform == "lowercase":
                df[column] = df[column].astype(str).str.lower()
            elif transform == "uppercase":
                df[column] = df[column].astype(str).str.upper()
            elif transform == "strip":
                df[column] = df[column].astype(str).str.strip()

            if col_type == "decimal":
                df[column] = pd.to_numeric(df[column], errors="coerce")
            elif col_type == "integer":
                df[column] = pd.to_numeric(df[column], errors="coerce").astype("Int64")
            elif col_type == "string":
                df[column] = df[column].astype(str)

        output_dir = os.path.dirname(output_path)
        if output_dir:
            os.makedirs(output_dir, exist_ok=True)

        df.to_csv(output_path, index=False, encoding=encoding)

        return {"rows_processed": len(df), "output_path": raw_output}
