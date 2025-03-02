import pytest
from unittest.mock import MagicMock, patch
from fastapi.testclient import TestClient

# Patch the database engine before importing the app so no real DB is required.
_mock_engine = MagicMock()
_mock_engine.connect.return_value.__enter__ = MagicMock(return_value=_mock_engine)
_mock_engine.connect.return_value.__exit__ = MagicMock(return_value=False)


@pytest.fixture(scope="session", autouse=True)
def patch_engine():
    with patch("app.database.engine", _mock_engine):
        yield _mock_engine


@pytest.fixture(scope="session")
def test_client():
    with (
        patch("app.database.engine", _mock_engine),
        patch("sqlalchemy.orm.Session", MagicMock()),
    ):
        from app.main import app
        with TestClient(app, raise_server_exceptions=False) as client:
            yield client


@pytest.fixture()
def mock_db_session():
    """Return a MagicMock acting as a SQLAlchemy Session."""
    session = MagicMock()
    session.query.return_value.filter.return_value.order_by.return_value.first.return_value = None
    return session
