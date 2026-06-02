from fastapi import APIRouter
from fastapi.responses import JSONResponse

from app.schemas.analyze_schema import AnalyzeRequest, AnalyzeResponse
from app.services import download_service, essentia_service

router = APIRouter(prefix="/api/ai", tags=["analyze"])

# 분석을 건너뛸 최소 재생 시간 (초)
MIN_DURATION_SECONDS = 120


@router.post("/analyze", response_model=AnalyzeResponse)
async def analyze_music(request: AnalyzeRequest):
    """
    YouTube 영상의 음악 feature를 분석한다.

    동작 흐름:
        1. duration_seconds < 120  → SKIPPED 응답 반환 (다운로드/분석 없음)
        2. duration_seconds >= 120 → yt-dlp로 오디오 다운로드
        3. 다운로드된 파일을 Essentia로 분석
        4. 핵심 feature + essentia_features JSON 반환
        5. 실패 시 FAILED 응답 반환
    """

    # ── 1. 길이 검사 ──────────────────────────────────────────────────────────
    if request.duration_seconds < MIN_DURATION_SECONDS:
        return AnalyzeResponse(
            youtube_video_id=request.youtube_video_id,
            title=request.title,
            status="SKIPPED",
            message=f"재생 시간({request.duration_seconds}초)이 {MIN_DURATION_SECONDS}초 미만이므로 분석을 건너뜁니다.",
            duration_seconds=request.duration_seconds,
        )

    # ── 2. 오디오 다운로드 ────────────────────────────────────────────────────
    try:
        audio_path = download_service.download_audio(
            youtube_url=request.youtube_url,
            youtube_video_id=request.youtube_video_id,
        )
    except RuntimeError as e:
        return AnalyzeResponse(
            youtube_video_id=request.youtube_video_id,
            title=request.title,
            status="FAILED",
            message=f"다운로드 실패: {e}",
            duration_seconds=request.duration_seconds,
        )

    # ── 3. Essentia 분석 ──────────────────────────────────────────────────────
    try:
        features = essentia_service.analyze_audio(audio_path)
    except RuntimeError as e:
        return AnalyzeResponse(
            youtube_video_id=request.youtube_video_id,
            title=request.title,
            status="FAILED",
            message=f"분석 실패: {e}",
            audio_path=audio_path,
            duration_seconds=request.duration_seconds,
        )

    # ── 4. 성공 응답 반환 ─────────────────────────────────────────────────────
    return AnalyzeResponse(
        youtube_video_id=request.youtube_video_id,
        title=request.title,
        status="COMPLETED",
        message="분석 완료",
        audio_path=audio_path,
        duration_seconds=features["duration_seconds"],
        bpm=features["bpm"],
        energy=features["energy"],
        danceability=features["danceability"],
        loudness=features["loudness"],
        musical_key=features["musical_key"],
        musical_scale=features["musical_scale"],
        essentia_features=features["essentia_features"],
    )
