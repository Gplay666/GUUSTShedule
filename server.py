#!/usr/bin/env python3
"""Минимальный тестовый сайт расписания УУНиТ.

Сервер отдаёт статическую страницу и проксирует запросы к JSON API
dev.uust-time.ru. Прокси нужен из-за CORS: API разрешает браузерные
запросы только с https://schedule.uust.ru.
"""

from __future__ import annotations

import gzip
import json
import mimetypes
import os
import threading
import time
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import parse_qs, urlencode, urlparse
from urllib.request import HTTPRedirectHandler, Request, build_opener

ROOT = Path(__file__).resolve().parent
STATIC_DIR = ROOT / "static"

API_BASE = os.environ.get(
    "UUST_API_BASE", "https://dev.uust-time.ru/api/v/852972"
).rstrip("/")
DEFAULT_SEMESTER = os.environ.get("UUST_SEMESTER", "241")
SITE = "schedule"
UPSTREAM_TIMEOUT = 25

GROUPS_TTL = 12 * 60 * 60
SCHEDULE_TTL = 60 * 60

_cache: dict[str, tuple[float, object]] = {}
_cache_lock = threading.Lock()


class _NoRedirect(HTTPRedirectHandler):
    """Не следовать за редиректами API: они означают неверный путь."""

    def redirect_request(self, req, fp, code, msg, headers, newurl):  # noqa: ANN001
        return None


_opener = build_opener(_NoRedirect)


def cache_get(key: str) -> object | None:
    with _cache_lock:
        item = _cache.get(key)
        if item is None:
            return None
        expires_at, value = item
        if time.time() >= expires_at:
            _cache.pop(key, None)
            return None
        return value


def cache_set(key: str, value: object, ttl: int) -> None:
    with _cache_lock:
        _cache[key] = (time.time() + ttl, value)


def api_get(path: str, params: dict[str, str] | None = None) -> object:
    query = {"site": SITE}
    if params:
        query.update(params)
    url = f"{API_BASE}/{path.lstrip('/')}?{urlencode(query)}"
    request = Request(
        url,
        headers={
            "Accept": "application/json",
            "User-Agent": "UUSTScheduleTest/0.1",
        },
    )

    try:
        with _opener.open(request, timeout=UPSTREAM_TIMEOUT) as response:
            body = response.read()
    except HTTPError as exc:
        detail = exc.read().decode("utf-8", "replace").strip()[:500]
        raise RuntimeError(f"API вернул HTTP {exc.code}: {detail}") from exc
    except URLError as exc:
        raise RuntimeError(f"Не удалось подключиться к API: {exc.reason}") from exc
    except TimeoutError as exc:
        raise RuntimeError("API не ответил за отведённое время") from exc

    try:
        return json.loads(body.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise RuntimeError("API вернул не JSON") from exc


def get_groups() -> list[dict]:
    cached = cache_get("groups")
    if cached is not None:
        return cached  # type: ignore[return-value]

    data = api_get("groups")
    if not isinstance(data, list):
        raise RuntimeError("Справочник групп вернул не массив")

    groups = [
        {
            "group_id": item.get("group_id"),
            "group_title": item.get("group_title") or "",
            "faculty": item.get("faculty") or "",
            "course": item.get("course"),
        }
        for item in data
        if isinstance(item, dict) and item.get("group_id") is not None
    ]
    groups.sort(key=lambda item: (item["group_title"].casefold(), item["group_id"]))
    cache_set("groups", groups, GROUPS_TTL)
    return groups


def get_schedule(group_id: int, semester: str) -> list[dict]:
    cache_key = f"schedule:0:{group_id}:{semester}"
    cached = cache_get(cache_key)
    if cached is not None:
        return cached  # type: ignore[return-value]

    data = api_get(f"schedule/0/{group_id}/semester/{semester}")
    if not isinstance(data, list):
        raise RuntimeError("Расписание вернуло не массив")

    cache_set(cache_key, data, SCHEDULE_TTL)
    return data


class Handler(BaseHTTPRequestHandler):
    server_version = "UUSTScheduleTest/0.1"

    def do_GET(self) -> None:  # noqa: N802
        parsed = urlparse(self.path)
        if parsed.path == "/api/groups":
            self.handle_groups()
        elif parsed.path == "/api/schedule":
            self.handle_schedule(parse_qs(parsed.query))
        elif parsed.path == "/api/health":
            self.send_json({"ok": True, "api_base": API_BASE})
        else:
            self.serve_static(parsed.path)

    def handle_groups(self) -> None:
        try:
            groups = get_groups()
        except Exception as exc:  # noqa: BLE001
            self.send_json({"error": str(exc)}, HTTPStatus.BAD_GATEWAY)
            return
        self.send_json({"count": len(groups), "groups": groups})

    def handle_schedule(self, query: dict[str, list[str]]) -> None:
        raw_group_id = (query.get("group_id") or [""])[0].strip()
        try:
            group_id = int(raw_group_id)
        except ValueError:
            self.send_json({"error": "group_id должен быть числом"}, HTTPStatus.BAD_REQUEST)
            return

        if group_id <= 0:
            self.send_json({"error": "group_id должен быть положительным"}, HTTPStatus.BAD_REQUEST)
            return

        semester = (query.get("semester") or [DEFAULT_SEMESTER])[0].strip()
        if not semester.isdigit():
            self.send_json({"error": "semester должен быть числом"}, HTTPStatus.BAD_REQUEST)
            return

        try:
            lessons = get_schedule(group_id, semester)
        except Exception as exc:  # noqa: BLE001
            self.send_json({"error": str(exc)}, HTTPStatus.BAD_GATEWAY)
            return

        self.send_json(
            {
                "group_id": group_id,
                "semester": semester,
                "lessons": lessons,
            }
        )

    def serve_static(self, request_path: str) -> None:
        relative = request_path.lstrip("/") or "index.html"
        target = (STATIC_DIR / relative).resolve()

        try:
            target.relative_to(STATIC_DIR.resolve())
        except ValueError:
            self.send_error(HTTPStatus.NOT_FOUND)
            return

        if target.is_dir():
            target = target / "index.html"

        if not target.is_file():
            self.send_error(HTTPStatus.NOT_FOUND)
            return

        content_type = mimetypes.guess_type(target.name)[0] or "application/octet-stream"
        if content_type.startswith("text/") or content_type in {
            "application/javascript",
            "application/json",
        }:
            content_type += "; charset=utf-8"

        body = target.read_bytes()
        self.send_response(HTTPStatus.OK)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-cache")
        self.end_headers()
        self.send_bytes(body)

    def send_json(self, payload: object, status: HTTPStatus = HTTPStatus.OK) -> None:
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        extra_headers: dict[str, str] = {}
        if len(body) > 1024 and "gzip" in self.headers.get("Accept-Encoding", "").lower():
            body = gzip.compress(body, compresslevel=6)
            extra_headers["Content-Encoding"] = "gzip"
            extra_headers["Vary"] = "Accept-Encoding"

        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        for name, value in extra_headers.items():
            self.send_header(name, value)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.send_bytes(body)

    def send_bytes(self, body: bytes) -> None:
        """Отправить тело небольшими порциями.

        В тестовом окружении большой одиночный send() до localhost
        может потеряться; порционная отправка работает стабильно.
        В обычном окружении это просто немного более мелкая запись.
        """
        chunk_size = 2048
        for start in range(0, len(body), chunk_size):
            self.connection.sendall(body[start : start + chunk_size])
            if start + chunk_size < len(body):
                time.sleep(0.005)

    def log_message(self, fmt: str, *args: object) -> None:
        print(f"[{self.log_date_time_string()}] {fmt % args}")


def main() -> None:
    host = os.environ.get("HOST", "127.0.0.1")
    port = int(os.environ.get("PORT", "8000"))
    server = ThreadingHTTPServer((host, port), Handler)
    print(f"Тестовый сайт: http://{host}:{port}")
    print(f"API: {API_BASE}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nОстановлено")
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
