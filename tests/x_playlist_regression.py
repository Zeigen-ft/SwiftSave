"""Verify the X preview-card failure against real yt-dlp, without network/media."""
import copy
from unittest.mock import patch

from yt_dlp import YoutubeDL
from yt_dlp.utils import DownloadError


class QuietLogger:
    def debug(self, message): pass
    def warning(self, message): pass
    def error(self, message): pass


fixture = {
    "_type": "playlist", "id": "x-post", "title": "Synthetic post",
    "entries": [
        {"id": "real-video", "title": "Synthetic video", "formats": [
            {"format_id": "mp4", "url": "https://media.invalid/fixture.mp4",
             "ext": "mp4", "vcodec": "avc1", "acodec": "mp4a"}
        ]},
        {"id": "preview-card", "title": "Synthetic preview", "formats": []},
    ],
}
options = {
    "simulate": True, "skip_download": True, "quiet": True,
    "noplaylist": True, "check_formats": False, "logger": QuietLogger(),
}


def process(extra):
    with YoutubeDL({**options, **extra}) as ydl:
        with patch.object(ydl, "urlopen", side_effect=AssertionError("Network forbidden in fixture")):
            return ydl.process_ie_result(copy.deepcopy(fixture), download=False)


try:
    process({})
except DownloadError as error:
    assert "No video formats" in str(error), type(error).__name__
else:
    raise AssertionError("Baseline must reproduce the invalid preview-card failure")

result = process({"playlist_items": "1"})
entries = [entry for entry in result["entries"] if entry]
assert len(entries) == 1 and entries[0]["id"] == "real-video"
print("PASS: X preview-card failure reproduced; first-video selection succeeds without network")
