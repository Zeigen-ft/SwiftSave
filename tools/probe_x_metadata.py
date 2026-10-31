"""Collect non-graphic X access errors/counts. Never download media or images."""
import json
import os
import re
from pathlib import Path

from yt_dlp import YoutubeDL
from yt_dlp.utils import DownloadError


class QuietLogger:
    def debug(self, message): pass
    def warning(self, message): pass
    def error(self, message): pass


def probe(name, url, first_only):
    options = {
        "simulate": True, "skip_download": True, "quiet": True,
        "noplaylist": True, "logger": QuietLogger(), "socket_timeout": 15,
        "retries": 0, "extractor_retries": 0, "ignoreerrors": False,
        "writethumbnail": False, "writesubtitles": False,
        "writeautomaticsub": False,
    }
    if first_only:
        options["playlist_items"] = "1"
    try:
        with YoutubeDL(options) as ydl:
            info = ydl.extract_info(url, download=False)
        entries = list(info.get("entries") or [info])
        return {"case": name, "success": True, "entries": [
            {"id": entry.get("id"), "format_count": len(entry.get("formats") or []),
             "availability": entry.get("availability")}
            for entry in entries if entry
        ]}
    except DownloadError as error:
        # No titles, descriptions, cookies, thumbnails, or media URLs in reports.
        message = re.sub(r"https?://\S+", "[link]", str(error))
        message = re.sub(r"\x1b\[[0-9;]*m", "", message)
        return {"case": name, "success": False, "error": message[:600]}


target = os.environ["X_PROBE_URL"]
control = "https://x.com/Twitter/status/560070183650213889"
results = [probe("reported-post-default", target, False),
           probe("reported-post-first-video", target, True),
           probe("public-control-first-video", control, True)]
Path("x-metadata-results.json").write_text(json.dumps(results, indent=2) + "\n")
print(json.dumps(results, indent=2))
