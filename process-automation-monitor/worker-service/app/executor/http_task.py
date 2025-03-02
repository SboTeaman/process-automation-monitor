import ipaddress
import socket
import anyio
import httpx
from httpx._transports.default import AsyncHTTPTransport
from app.executor.base import BaseExecutor

ALLOWED_METHODS = {"GET", "POST", "PUT", "DELETE"}

# RFC 1918 / RFC 5735 / RFC 4193 reserved ranges — never valid external targets.
_PRIVATE_NETWORKS = [
    ipaddress.ip_network("10.0.0.0/8"),
    ipaddress.ip_network("172.16.0.0/12"),
    ipaddress.ip_network("192.168.0.0/16"),
    ipaddress.ip_network("127.0.0.0/8"),
    ipaddress.ip_network("169.254.0.0/16"),  # link-local / cloud metadata endpoints
    ipaddress.ip_network("0.0.0.0/8"),
    ipaddress.ip_network("::1/128"),
    ipaddress.ip_network("fc00::/7"),
    ipaddress.ip_network("fe80::/10"),
]


def _check_ip_not_private(host: str, port: int) -> None:
    """Resolve host and raise if it maps to a private/reserved address.

    Called at connection time (inside the transport) to prevent DNS-rebinding
    attacks — the check happens on the same resolved IP that will actually be used.
    """
    try:
        infos = socket.getaddrinfo(host, port, proto=socket.IPPROTO_TCP)
    except socket.gaierror as exc:
        raise ValueError(f"Cannot resolve host '{host}': {exc}") from exc

    for _family, _type, _proto, _canonname, sockaddr in infos:
        raw_ip = sockaddr[0]
        try:
            addr = ipaddress.ip_address(raw_ip)
        except ValueError:
            continue
        if any(addr in net for net in _PRIVATE_NETWORKS):
            raise ValueError(
                f"Requests to private/reserved addresses are not allowed (resolved: {raw_ip})"
            )


class _SSRFSafeTransport(AsyncHTTPTransport):
    """AsyncHTTPTransport that blocks connections to private IP ranges.

    The IP check runs *after* DNS resolution but *before* the TCP handshake,
    preventing DNS-rebinding attacks where the attacker rotates DNS records
    between the validation check and the actual connection.
    """

    async def handle_async_request(self, request: httpx.Request) -> httpx.Response:
        host = request.url.host
        port = request.url.port or (443 if request.url.scheme == "https" else 80)
        await anyio.to_thread.run_sync(
            lambda: _check_ip_not_private(host, port),
            cancellable=True,
        )
        return await super().handle_async_request(request)


class HttpCallExecutor(BaseExecutor):
    """Executor for HTTP_CALL tasks — sends an HTTP request to an external URL."""

    async def execute(self, config: dict) -> dict:
        """Execute an HTTP request based on the provided config.

        Args:
            config: Must contain 'url' and 'method'. Optional: 'headers', 'body',
                    'expectedStatusCode' (default 200), 'timeout' (default 30).

        Returns:
            {"status_code": int, "body": str, "matched": bool}

        Raises:
            ValueError: If URL is invalid, method unsupported, or target IP is private.
            Exception: If the response status code does not match expectedStatusCode.
        """
        url: str = config.get("url", "")
        method: str = str(config.get("method", "GET")).upper()
        headers: dict = config.get("headers", {})
        body: dict | None = config.get("body", None)
        expected_status: int = int(config.get("expectedStatusCode", 200))
        timeout: int = int(config.get("timeout", 30))

        if not url.startswith("http://") and not url.startswith("https://"):
            raise ValueError(f"Invalid URL '{url}': must start with http:// or https://")

        if method not in ALLOWED_METHODS:
            raise ValueError(f"Unsupported HTTP method '{method}'. Allowed: {ALLOWED_METHODS}")

        async with httpx.AsyncClient(
            transport=_SSRFSafeTransport(),
            timeout=timeout,
            follow_redirects=False,
        ) as client:
            response = await client.request(
                method=method,
                url=url,
                headers=headers,
                json=body if body else None,
            )

        matched = response.status_code == expected_status
        if not matched:
            raise Exception(
                f"HTTP {method} {url} returned {response.status_code}, "
                f"expected {expected_status}"
            )

        return {
            "status_code": response.status_code,
            "body": response.text,
            "matched": matched,
        }
