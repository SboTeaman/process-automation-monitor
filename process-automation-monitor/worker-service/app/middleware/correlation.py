import uuid
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import Response

CORRELATION_HEADER = "X-Correlation-Id"


class CorrelationMiddleware(BaseHTTPMiddleware):
    """Middleware that ensures every request carries an X-Correlation-Id header.

    If the incoming request does not provide the header, a new UUID4 is generated.
    The correlation ID is stored in request.state.correlation_id and added to
    the response headers.
    """

    async def dispatch(self, request: Request, call_next) -> Response:
        correlation_id = request.headers.get(CORRELATION_HEADER)
        if not correlation_id:
            correlation_id = str(uuid.uuid4())

        request.state.correlation_id = correlation_id
        response: Response = await call_next(request)
        response.headers[CORRELATION_HEADER] = correlation_id
        return response
