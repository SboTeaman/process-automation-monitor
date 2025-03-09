from __future__ import annotations

from typing import Generator

from sqlalchemy import create_engine
from sqlalchemy.engine import Engine
from sqlalchemy.orm import sessionmaker, Session

from app.config import settings

# ---------------------------------------------------------------------------
# Lazy engine construction — deferred until first use so tests can patch
# ANALYTICS_DB_URL before any real connection is attempted.
# ---------------------------------------------------------------------------
_analytics_engine: Engine | None = None
_analytics_session_factory = None


def _get_analytics_engine() -> Engine:
    global _analytics_engine, _analytics_session_factory
    if _analytics_engine is None:
        _analytics_engine = create_engine(
            settings.ANALYTICS_DB_URL,
            pool_pre_ping=True,
            pool_size=5,
            max_overflow=10,
        )
        _analytics_session_factory = sessionmaker(
            autocommit=False,
            autoflush=False,
            bind=_analytics_engine,
        )
    return _analytics_engine


# Expose analytics_engine as a module-level property for alembic / main.py
class _EngineProxy:
    """Proxy that forwards attribute access to the lazy engine."""

    def __getattr__(self, item):
        return getattr(_get_analytics_engine(), item)


analytics_engine = _EngineProxy()


def _get_session_factory():
    _get_analytics_engine()  # ensure initialised
    return _analytics_session_factory


# Kept for backward-compat (ETL imports this)
AnalyticsSessionLocal: sessionmaker  # type: ignore[assignment]


class _SessionFactoryProxy:
    def __call__(self, *args, **kwargs):
        return _get_session_factory()(*args, **kwargs)


AnalyticsSessionLocal = _SessionFactoryProxy()  # type: ignore[assignment]


# Orchestrator PostgreSQL engine (ETL source — created lazily)
def create_orchestrator_engine() -> Engine:
    return create_engine(
        settings.ORCHESTRATOR_DB_URL,
        pool_pre_ping=True,
        pool_size=2,
        max_overflow=5,
    )


def get_db() -> Generator[Session, None, None]:
    """FastAPI dependency: yields an analytics MySQL session."""
    db = _get_session_factory()()
    try:
        yield db
    finally:
        db.close()
