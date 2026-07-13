#!/usr/bin/env python3
"""
PathMemo Web Viewer

A tiny local web tool that pulls the track database from a connected Android
phone via ADB and displays the trajectories in a browser.

Usage:
    python web_viewer.py

Then open http://localhost:8765 in your browser.
"""

import base64
import json
import os
import shutil
import sqlite3
import subprocess
import sys
import tempfile
import time
import webbrowser
from http.server import HTTPServer, SimpleHTTPRequestHandler
from pathlib import Path
from threading import Thread

HERE = Path(__file__).parent.resolve()
PROJECT_ROOT = HERE.parent.parent
PORT = 8765


def load_amap_key():
    """Read the AMap API key from the Android project's local.properties."""
    local_props = PROJECT_ROOT / "local.properties"
    if local_props.exists():
        for line in local_props.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if line.startswith("AMAP_API_KEY="):
                return line.split("=", 1)[1].strip()
    return ""


def run_adb(*args):
    """Run an adb command and return stdout."""
    adb = shutil.which("adb")
    if adb is None:
        raise RuntimeError("adb not found in PATH. Please install Android SDK platform-tools.")
    result = subprocess.run(
        [adb, *args],
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    if result.returncode != 0:
        raise RuntimeError(f"adb failed: {result.stderr.strip()}")
    return result.stdout.strip()


def check_device():
    """Check that exactly one Android device is connected."""
    output = run_adb("devices")
    lines = [line.strip() for line in output.splitlines() if line.strip()]
    devices = [line for line in lines if not line.startswith("List of") and "device" in line]
    if not devices:
        raise RuntimeError("No Android device connected. Please connect your phone and enable USB debugging.")
    if len(devices) > 1:
        raise RuntimeError(f"Multiple devices connected:\n{output}")
    return devices[0].split()[0]


def pull_database():
    """Pull the PathMemo SQLite database from the phone."""
    device = check_device()
    print(f"Device: {device}")

    remote_db = "databases/pathmemo_database"
    local_dir = tempfile.mkdtemp(prefix="pathmemo_viewer_")
    local_db = os.path.join(local_dir, "pathmemo_database")

    print(f"Pulling database to {local_db} ...")
    # Android debug bridge can corrupt raw binary on Windows, so we transfer
    # the SQLite file base64-encoded and decode it locally.
    adb = shutil.which("adb")
    result = subprocess.run(
        [adb, "shell", f"run-as com.pathmemo cat {remote_db} | base64"],
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=False,
    )
    if result.returncode != 0:
        raise RuntimeError(f"Failed to pull database. Is the PathMemo app installed and debuggable?\n{result.stderr.strip()}")

    raw = base64.b64decode(result.stdout.encode("ascii"))
    if len(raw) < 16 or raw[:16] != b"SQLite format 3\x00":
        raise RuntimeError("Downloaded database is corrupted. Try reconnecting the device.")

    with open(local_db, "wb") as f:
        f.write(raw)
    return local_db


def export_data(db_path):
    """Read tracks and total point count from the SQLite database."""
    conn = sqlite3.connect(db_path)
    conn.row_factory = sqlite3.Row
    cur = conn.cursor()

    cur.execute("""
        SELECT id, name, startTime, endTime, distanceMeters, pointCount
        FROM tracks
        ORDER BY startTime ASC
    """)
    tracks = [dict(row) for row in cur.fetchall()]

    cur.execute("SELECT COUNT(*) as count FROM location_points")
    point_count = cur.fetchone()["count"]

    conn.close()

    return {
        "exportedAt": int(time.time() * 1000),
        "trackCount": len(tracks),
        "pointCount": point_count,
        "tracks": tracks,
    }


def query_points(db_path, start, end):
    """Query location points within a timestamp range."""
    conn = sqlite3.connect(db_path)
    conn.row_factory = sqlite3.Row
    cur = conn.cursor()
    cur.execute("""
        SELECT id, trackId, latitude, longitude, altitude, accuracy, speed, timestamp
        FROM location_points
        WHERE timestamp >= ? AND timestamp <= ?
        ORDER BY timestamp ASC
    """, (start, end))
    points = [dict(row) for row in cur.fetchall()]
    conn.close()
    return points


def query_daily_summary(db_path):
    """Return daily point counts and time ranges for the calendar heatmap."""
    conn = sqlite3.connect(db_path)
    conn.row_factory = sqlite3.Row
    cur = conn.cursor()
    cur.execute("""
        SELECT timestamp FROM location_points ORDER BY timestamp ASC
    """)
    rows = cur.fetchall()
    conn.close()

    daily = {}
    for row in rows:
        ts = row["timestamp"]
        d = time.gmtime(ts / 1000)
        key = f"{d.tm_year}-{d.tm_mon:02d}"
        day = d.tm_mday
        if key not in daily:
            daily[key] = {}
        if day not in daily[key]:
            daily[key][day] = {"count": 0, "first": ts, "last": ts}
        daily[key][day]["count"] += 1
        if ts < daily[key][day]["first"]:
            daily[key][day]["first"] = ts
        if ts > daily[key][day]["last"]:
            daily[key][day]["last"] = ts

    # Convert duration in ms
    for month in daily.values():
        for day in month.values():
            day["duration"] = day["last"] - day["first"]

    return daily


def query_all_tracks(db_path):
    """Return all tracks for reference."""
    conn = sqlite3.connect(db_path)
    conn.row_factory = sqlite3.Row
    cur = conn.cursor()
    cur.execute("""
        SELECT id, name, startTime, endTime, distanceMeters, pointCount
        FROM tracks
        ORDER BY startTime ASC
    """)
    tracks = [dict(row) for row in cur.fetchall()]
    conn.close()
    return tracks


class Handler(SimpleHTTPRequestHandler):
    """Serve static files and API endpoints."""

    def __init__(self, *args, data=None, config=None, db_path=None, **kwargs):
        self.data = data
        self.config = config or {}
        self.db_path = db_path
        super().__init__(*args, directory=str(HERE), **kwargs)

    def _send_json(self, obj):
        body = json.dumps(obj, ensure_ascii=False).encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        if self.path == "/api/data.json":
            self._send_json(self.data)
            return
        if self.path == "/api/config.json":
            self._send_json(self.config)
            return
        if self.path == "/api/daily.json":
            daily = query_daily_summary(self.db_path)
            self._send_json(daily)
            return
        if self.path.startswith("/api/points.json"):
            from urllib.parse import parse_qs, urlparse
            qs = parse_qs(urlparse(self.path).query)
            try:
                start = int(qs.get("start", [0])[0])
                end = int(qs.get("end", [int(time.time() * 1000)])[0])
            except (ValueError, TypeError):
                self.send_error(400, "Invalid start/end parameters")
                return
            points = query_points(self.db_path, start, end)
            self._send_json({
                "start": start,
                "end": end,
                "count": len(points),
                "points": points
            })
            return
        if self.path == "/":
            self.path = "/index.html"
        return super().do_GET()

    def log_message(self, format, *args):
        # Quieter logging
        pass


def main():
    if len(sys.argv) > 1:
        db_path = sys.argv[1]
        if not os.path.isfile(db_path):
            print(f"Error: file not found: {db_path}")
            sys.exit(1)
        print(f"Using local database: {db_path}")
    else:
        try:
            db_path = pull_database()
        except RuntimeError as e:
            print(f"Error: {e}")
            sys.exit(1)

    data = export_data(db_path)
    print(f"Loaded {data['trackCount']} tracks, {data['pointCount']} points.")

    amap_key = load_amap_key()
    config = {"amapKey": amap_key}
    if not amap_key:
        print("Warning: AMAP_API_KEY not found in local.properties; map tiles may fail to load.")

    def handler_factory(*args, **kwargs):
        return Handler(*args, data=data, config=config, db_path=db_path, **kwargs)

    server = HTTPServer(("127.0.0.1", PORT), handler_factory)
    url = f"http://127.0.0.1:{PORT}/"
    print(f"Serving at {url}")

    Thread(target=server.serve_forever, daemon=True).start()
    time.sleep(0.5)

    # Open the browser without blocking the server. In headless environments
    # webbrowser.open() can hang, so we run it in a short-lived thread.
    if os.environ.get("WEB_VIEWER_NO_BROWSER") != "1":
        def _open_browser():
            try:
                webbrowser.open(url)
            except Exception as e:
                print(f"Could not open browser: {e}")
        Thread(target=_open_browser, daemon=True).start()
        print(f"Opening browser... {url}")
    else:
        print(f"Browser auto-open disabled. Open {url} manually.")

    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        print("\nShutting down...")
        server.shutdown()


if __name__ == "__main__":
    main()
