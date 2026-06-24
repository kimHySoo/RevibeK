import os
import yt_dlp
from pathlib import Path

# 다운로드 저장 폴더 (프로젝트 루트 기준)
DOWNLOADS_DIR = Path("downloads")


def _build_proxy_url() -> str | None:
    """환경변수로 주거용 프록시(Residential Proxy) 접속 정보를 구성한다. 미설정 시 None."""
    host = os.environ.get("PROXY_HOST")
    port = os.environ.get("PROXY_PORT")
    username = os.environ.get("PROXY_USERNAME")
    password = os.environ.get("PROXY_PASSWORD")
    if not host or not port:
        return None
    if username and password:
        return f"http://{username}:{password}@{host}:{port}"
    return f"http://{host}:{port}"


def download_audio(youtube_url: str, youtube_video_id: str) -> str:
    """
    YouTube URL에서 오디오를 다운로드하고 저장된 파일의 경로를 반환한다.

    :param youtube_url: 다운로드할 YouTube URL
    :param youtube_video_id: 파일명에 사용할 YouTube 영상 ID
    :return: 다운로드된 오디오 파일의 경로 문자열
    :raises RuntimeError: 다운로드 실패 또는 파일 탐색 실패 시
    """
    DOWNLOADS_DIR.mkdir(exist_ok=True)

    # 저장 경로 템플릿: downloads/{youtube_video_id}.{ext}
    output_template = str(DOWNLOADS_DIR / f"{youtube_video_id}.%(ext)s")

    ydl_opts = {
        # 최고 품질 오디오만 선택
        "format": "bestaudio/best",
        "outtmpl": output_template,
        # 재생목록이 넘어와도 단일 영상만 처리
        "noplaylist": True,
        # FFmpeg로 wav 변환
        "postprocessors": [
            {
                "key": "FFmpegExtractAudio",
                "preferredcodec": "wav",
            }
        ],
    }

    proxy_url = _build_proxy_url()
    if proxy_url:
        ydl_opts["proxy"] = proxy_url

    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            ydl.download([youtube_url])
    except Exception as e:
        raise RuntimeError(f"yt-dlp 다운로드 실패 (video_id={youtube_video_id}): {e}")

    # wav → 실패 시 mp3, m4a, webm 순서로 탐색
    for ext in ["wav", "mp3", "m4a", "webm", "opus"]:
        candidate = DOWNLOADS_DIR / f"{youtube_video_id}.{ext}"
        if candidate.exists():
            return str(candidate)

    raise RuntimeError(
        f"다운로드 후 파일을 찾을 수 없습니다 (video_id={youtube_video_id}). "
        "FFmpeg가 설치되어 있는지 확인하세요."
    )
