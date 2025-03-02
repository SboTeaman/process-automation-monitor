import re
import httpx
from app.executor.base import BaseExecutor

EMAIL_REGEX = re.compile(r"^[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}$")


def _validate_record(record: dict, rules: list) -> list[dict]:
    """Validate a single record against the provided rules.

    Returns a list of error dicts with 'field' and 'message' keys.
    """
    errors: list[dict] = []

    for rule in rules:
        field = rule.get("field")
        value = record.get(field)

        required = rule.get("required", False)
        if required and (value is None or str(value).strip() == ""):
            errors.append({"field": field, "message": f"Field '{field}' is required."})
            continue

        if value is None:
            continue

        fmt = rule.get("format")
        if fmt == "email" and not EMAIL_REGEX.match(str(value)):
            errors.append({"field": field, "message": f"Field '{field}' is not a valid email."})

        min_val = rule.get("min")
        max_val = rule.get("max")
        if min_val is not None or max_val is not None:
            try:
                numeric = float(value)
                if min_val is not None and numeric < min_val:
                    errors.append(
                        {"field": field, "message": f"Field '{field}' is below minimum {min_val}."}
                    )
                if max_val is not None and numeric > max_val:
                    errors.append(
                        {"field": field, "message": f"Field '{field}' exceeds maximum {max_val}."}
                    )
            except (TypeError, ValueError):
                errors.append(
                    {"field": field, "message": f"Field '{field}' cannot be converted to a number."}
                )

    return errors


class DataValidateExecutor(BaseExecutor):
    """Executor for DATA_VALIDATE tasks — validates records against field rules."""

    async def execute(self, config: dict) -> dict:
        """Validate data records according to the provided rules.

        Args:
            config: Must contain 'dataSource' ('mysql' or 'api') and 'rules'.
                    If dataSource is 'api', must also contain 'endpoint'.

        Returns:
            {"total_records": int, "valid": int, "invalid": int, "errors": list}

        Raises:
            ValueError: If dataSource is unsupported or endpoint is missing for 'api'.
            httpx.HTTPError: If the API endpoint cannot be reached.
        """
        data_source: str = config.get("dataSource", "api")
        endpoint: str | None = config.get("endpoint")
        rules: list = config.get("rules", [])

        records: list[dict] = []

        if data_source == "api":
            if not endpoint:
                raise ValueError("'endpoint' is required when dataSource is 'api'.")
            async with httpx.AsyncClient(timeout=30) as client:
                response = await client.get(endpoint)
                response.raise_for_status()
                data = response.json()
                if isinstance(data, list):
                    records = data
                elif isinstance(data, dict):
                    records = [data]
                else:
                    records = []
        elif data_source == "mysql":
            records = config.get("data", [])
        else:
            raise ValueError(f"Unsupported dataSource '{data_source}'. Use 'mysql' or 'api'.")

        all_errors: list[dict] = []
        valid_count = 0
        invalid_count = 0

        for record in records:
            record_errors = _validate_record(record, rules)
            if record_errors:
                invalid_count += 1
                all_errors.extend(record_errors)
            else:
                valid_count += 1

        return {
            "total_records": len(records),
            "valid": valid_count,
            "invalid": invalid_count,
            "errors": all_errors,
        }
