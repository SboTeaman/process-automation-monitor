import logging
import time
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request, Response
from fastapi.responses import JSONResponse
from pythonjsonlogger import jsonlogger

from app.config import settings
from app.database import Base, engine
from app.middleware.api_key import ApiKeyMiddleware
from app.middleware.correlation import CorrelationMiddleware
from app.routes.execute import router as execute_router
from app.routes.health import router as health_router


def _configure_logging() -> None:
    handler = logging.StreamHandler()
    formatter = jsonlogger.JsonFormatter(
        fmt="%(asctime)s %(name)s %(levelname)s %(message)s",
        datefmt="%Y-%m-%dT%H:%M:%S",
    )
    handler.setFormatter(formatter)
    root_logger = logging.getLogger()
    root_logger.setLevel(settings.LOG_LEVEL.upper())
    root_logger.handlers = [handler]


_configure_logging()
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Starting %s — creating database tables", settings.SERVICE_NAME)
    Base.metadata.create_all(bind=engine)
    yield
    logger.info("Shutting down %s", settings.SERVICE_NAME)


app = FastAPI(
    title=settings.SERVICE_NAME,
    description="Worker service that executes automation jobs (HTTP, CSV, Validate, Report).",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(ApiKeyMiddleware)
app.add_middleware(CorrelationMiddleware)

app.include_router(health_router)
app.include_router(execute_router)


@app.middleware("http")
async def request_logging_middleware(request: Request, call_next) -> Response:
    start = time.perf_counter()
    response: Response = await call_next(request)
    duration_ms = int((time.perf_counter() - start) * 1000)
    correlation_id = getattr(request.state, "correlation_id", "")
    logger.info(
        "request",
        extra={
            "method": request.method,
            "path": request.url.path,
            "status": response.status_code,
            "duration_ms": duration_ms,
            "correlationId": correlation_id,
        },
    )
    return response


@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception) -> JSONResponse:
    from datetime import datetime, timezone
    correlation_id = getattr(request.state, "correlation_id", "")
    logger.exception("Unhandled exception: %s", str(exc))
    return JSONResponse(
        status_code=500,
        content={
            "error": "Internal server error",
            "code": "INTERNAL_ERROR",
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "correlationId": correlation_id,
        },
    )
