import pytest
import httpx
import respx
from app.executor.validate_task import DataValidateExecutor


RULES_EMAIL_AGE = [
    {"field": "email", "required": True, "format": "email"},
    {"field": "age", "min": 0, "max": 150},
]


@pytest.mark.asyncio
async def test_required_field_missing():
    """A record missing a required field should be counted as invalid."""
    executor = DataValidateExecutor()
    result = await executor.execute(
        {
            "dataSource": "mysql",
            "rules": [{"field": "email", "required": True}],
            "data": [{"email": ""}, {"email": "valid@test.com"}],
        }
    )
    assert result["total_records"] == 2
    assert result["invalid"] == 1
    assert result["valid"] == 1


@pytest.mark.asyncio
async def test_format_email_valid():
    """A valid email should pass the email format rule."""
    executor = DataValidateExecutor()
    result = await executor.execute(
        {
            "dataSource": "mysql",
            "rules": [{"field": "email", "required": True, "format": "email"}],
            "data": [{"email": "user@domain.com"}],
        }
    )
    assert result["valid"] == 1
    assert result["invalid"] == 0


@pytest.mark.asyncio
async def test_format_email_invalid():
    """A malformed email should fail the format rule."""
    executor = DataValidateExecutor()
    result = await executor.execute(
        {
            "dataSource": "mysql",
            "rules": [{"field": "email", "format": "email"}],
            "data": [{"email": "not-an-email"}],
        }
    )
    assert result["invalid"] == 1
    assert any("email" in e["field"] for e in result["errors"])


@pytest.mark.asyncio
async def test_age_range_violated_above_max():
    """An age above 150 should trigger a range violation."""
    executor = DataValidateExecutor()
    result = await executor.execute(
        {
            "dataSource": "mysql",
            "rules": [{"field": "age", "min": 0, "max": 150}],
            "data": [{"age": 200}],
        }
    )
    assert result["invalid"] == 1
    assert result["errors"][0]["field"] == "age"


@pytest.mark.asyncio
async def test_age_range_violated_below_min():
    """An age below 0 should trigger a range violation."""
    executor = DataValidateExecutor()
    result = await executor.execute(
        {
            "dataSource": "mysql",
            "rules": [{"field": "age", "min": 0, "max": 150}],
            "data": [{"age": -5}],
        }
    )
    assert result["invalid"] == 1


@pytest.mark.asyncio
async def test_api_datasource_fetches_data():
    """When dataSource is 'api', data should be fetched from the endpoint."""
    with respx.mock:
        respx.get("https://api.test/records").mock(
            return_value=httpx.Response(
                200, json=[{"email": "a@b.com"}, {"email": "bad-email"}]
            )
        )
        executor = DataValidateExecutor()
        result = await executor.execute(
            {
                "dataSource": "api",
                "endpoint": "https://api.test/records",
                "rules": [{"field": "email", "format": "email"}],
            }
        )
    assert result["total_records"] == 2
    assert result["invalid"] == 1


@pytest.mark.asyncio
async def test_missing_endpoint_raises():
    """api dataSource without endpoint should raise ValueError."""
    executor = DataValidateExecutor()
    with pytest.raises(ValueError, match="endpoint"):
        await executor.execute({"dataSource": "api", "rules": []})
