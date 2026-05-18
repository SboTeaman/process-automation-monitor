import secrets
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import JSONResponse

from app.config import settings

_EXEMPT_PATHS = {"/health", "/health/"}


class ApiKeyMiddleware(BaseHTTPMiddleware):
    """Require X-Worker-Api-Key header on all non-health endpoints.

    The key is compared with secrets.compare_digest to prevent timing attacks.
    If WORKER_API_KEY is empty the service starts but immediately rejects every
    protected request — this surfaces misconfiguration early rather than silently
    allowing unauthenticated access.
    """

    async def dispatch(self, request: Request, call_next):
        if request.url.path in _EXEMPT_PATHS:
            return await call_next(request)

        if not settings.WORKER_API_KEY:
            return JSONResponse(
                status_code=503,
                content={"error": "Service not configured: WORKER_API_KEY is not set."},
            )

        incoming = request.headers.get("X-Worker-Api-Key", "")
        if not secrets.compare_digest(incoming, settings.WORKER_API_KEY):
            return JSONResponse(
                status_code=401,
                content={"error": "Unauthorized", "code": "INVALID_API_KEY"},
            )

        return await call_next(request)
