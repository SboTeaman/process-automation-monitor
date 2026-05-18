import json
import logging
import secrets
import time
import uuid
from datetime import datetime, timezone

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware

from app.config import settings
from app.database import analytics_engine
from app.models import Base
from app.routes.health import router as health_router
from app.routes.stats import router as stats_router, reports_router

# ---------------------------------------------------------------------------
# Structured JSON logging setup
# ---------------------------------------------------------------------------
logging.basicConfig(
    level=getattr(logging, settings.LOG_LEVEL.upper(), logging.INFO),
    format="%(message)s",
)
logger = logging.getLogger(settings.SERVICE_NAME)


def _json_log(level: str, message: str, **kwargs):
    record = {
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "level": level,
        "service": settings.SERVICE_NAME,
        "message": message,
        **kwargs,
    }
    print(json.dumps(record), flush=True)


# ---------------------------------------------------------------------------
# FastAPI application
# ---------------------------------------------------------------------------
_ANALYTICS_EXEMPT = {"/health", "/health/"}

app = FastAPI(
    title="Analytics Service",
    description="Process Automation Monitor — Analytics & Statistics API",
    version="1.0.0",
    docs_url=None,
    redoc_url=None,
    openapi_url=None,
)


# ---------------------------------------------------------------------------
# Startup: ensure analytics tables exist
# ---------------------------------------------------------------------------
@app.on_event("startup")
def on_startup():
    _json_log("INFO", "Starting analytics-service, creating tables if not exist")
    Base.metadata.create_all(bind=analytics_engine)
    _json_log("INFO", "Analytics tables ready")


# ---------------------------------------------------------------------------
# API key auth middleware — analytics-service is internal only
# ---------------------------------------------------------------------------
@app.middleware("http")
async def api_key_middleware(request: Request, call_next):
    if request.url.path in _ANALYTICS_EXEMPT:
        return await call_next(request)

    if not settings.ANALYTICS_API_KEY:
        return JSONResponse(
            status_code=503,
            content={"error": "Service not configured: ANALYTICS_API_KEY is not set."},
        )

    incoming = request.headers.get("X-Analytics-Api-Key", "")
    if not secrets.compare_digest(incoming, settings.ANALYTICS_API_KEY):
        return JSONResponse(
            status_code=401,
            content={"error": "Unauthorized", "code": "INVALID_API_KEY"},
        )

    return await call_next(request)


# ---------------------------------------------------------------------------
# X-Correlation-Id middleware
# ---------------------------------------------------------------------------
@app.middleware("http")
async def correlation_id_middleware(request: Request, call_next):
    correlation_id = request.headers.get("X-Correlation-Id") or str(uuid.uuid4())
    request.state.correlation_id = correlation_id

    response = await call_next(request)
    response.headers["X-Correlation-Id"] = correlation_id
    return response


# ---------------------------------------------------------------------------
# Structured request logging middleware
# ---------------------------------------------------------------------------
@app.middleware("http")
async def request_logging_middleware(request: Request, call_next):
    start = time.monotonic()
    response = await call_next(request)
    duration_ms = round((time.monotonic() - start) * 1000, 2)

    correlation_id = getattr(request.state, "correlation_id", "")

    _json_log(
        "INFO",
        "HTTP request",
        method=request.method,
        path=request.url.path,
        status=response.status_code,
        duration_ms=duration_ms,
        correlationId=correlation_id,
    )
    return response


# ---------------------------------------------------------------------------
# Global error handler — unified error response format
# ---------------------------------------------------------------------------
@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    correlation_id = getattr(request.state, "correlation_id", str(uuid.uuid4()))
    _json_log(
        "ERROR",
        "Unhandled exception",
        error=str(exc),
        path=request.url.path,
        correlationId=correlation_id,
    )
    return JSONResponse(
        status_code=500,
        content={
            "error": "Internal server error",
            "code": "INTERNAL_SERVER_ERROR",
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "correlationId": correlation_id,
        },
    )


# ---------------------------------------------------------------------------
# Routers
# ---------------------------------------------------------------------------
app.include_router(health_router)
app.include_router(stats_router)
app.include_router(reports_router)
