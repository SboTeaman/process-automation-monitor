from datetime import datetime, timezone
from fastapi import APIRouter
from app.schemas import HealthResponse
from app.config import settings

router = APIRouter(tags=["health"])


@router.get("/health", response_model=HealthResponse)
async def health_check() -> HealthResponse:
    """Return the service health status."""
    return HealthResponse(
        status="UP",
        service=settings.SERVICE_NAME,
        timestamp=datetime.now(timezone.utc),
    )
