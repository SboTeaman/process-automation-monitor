import pytest
import httpx
import respx
from app.executor.http_task import HttpCallExecutor


@pytest.mark.asyncio
async def test_execute_get_success():
    """GET request that returns the expected status code should succeed."""
    with respx.mock:
        respx.get("https://example.com/api").mock(
            return_value=httpx.Response(200, text="ok")
        )
        executor = HttpCallExecutor()
        result = await executor.execute(
            {"url": "https://example.com/api", "method": "GET", "expectedStatusCode": 200}
        )
    assert result["status_code"] == 200
    assert result["matched"] is True
    assert result["body"] == "ok"


@pytest.mark.asyncio
async def test_execute_post_with_body():
    """POST request with body should include body in the call."""
    with respx.mock:
        route = respx.post("https://api.test/data").mock(
            return_value=httpx.Response(201, json={"id": 1})
        )
        executor = HttpCallExecutor()
        result = await executor.execute(
            {
                "url": "https://api.test/data",
                "method": "POST",
                "body": {"name": "test"},
                "expectedStatusCode": 201,
            }
        )
    assert result["status_code"] == 201
    assert result["matched"] is True
    assert route.called


@pytest.mark.asyncio
async def test_execute_wrong_status_raises():
    """A status code mismatch should raise an exception."""
    with respx.mock:
        respx.get("https://example.com/api").mock(
            return_value=httpx.Response(404, text="not found")
        )
        executor = HttpCallExecutor()
        with pytest.raises(Exception, match="404"):
            await executor.execute(
                {"url": "https://example.com/api", "method": "GET", "expectedStatusCode": 200}
            )


@pytest.mark.asyncio
async def test_execute_invalid_url_raises_value_error():
    """A URL without http/https scheme should raise ValueError."""
    executor = HttpCallExecutor()
    with pytest.raises(ValueError, match="Invalid URL"):
        await executor.execute({"url": "ftp://bad.url/path", "method": "GET"})


@pytest.mark.asyncio
async def test_execute_missing_scheme_raises():
    """A URL without any scheme should raise ValueError."""
    executor = HttpCallExecutor()
    with pytest.raises(ValueError, match="Invalid URL"):
        await executor.execute({"url": "example.com/api", "method": "GET"})


@pytest.mark.asyncio
async def test_execute_unsupported_method_raises():
    """An unsupported HTTP method should raise ValueError."""
    executor = HttpCallExecutor()
    with pytest.raises(ValueError, match="Unsupported HTTP method"):
        await executor.execute({"url": "https://example.com", "method": "PATCH"})
