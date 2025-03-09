from fastapi import APIRouter
from datetime import datetime, timezone

from app.schemas import HealthResponse
from app.config import settings

router = APIRouter(tags=["Health"])


@router.get("/health", response_model=HealthResponse)
def health_check():
    return HealthResponse(
        status="UP",
        service=settings.SERVICE_NAME,
        timestamp=datetime.now(timezone.utc).isoformat(),
    )
