import json
import logging
import os
from pathlib import Path

from fastapi import APIRouter

from app.schemas.analyze_schema import AnalyzeRequest, AnalyzeResponse
from app.services import download_service, embedding_service, essentia_service

router = APIRouter(prefix="/api/ai", tags=["analyze"])
logger = logging.getLogger(__name__)

# 분석을 건너뛸 최소 재생 시간 (초)
MIN_DURATION_SECONDS = 120

# 분석을 건너뛸 최대 재생 시간 (초)
MAX_DURATION_SECONDS = 600

# 분석 결과 JSON 저장 폴더 (오디오 다운로드와 동일 폴더 사용)
RESULTS_DIR = Path("downloads")


def _json_path(youtube_video_id: str) -> Path:
    """분석 결과 JSON 파일 경로를 반환한다."""
    return RESULTS_DIR / f"{youtube_video_id}.json"


def _save_result_json(youtube_video_id: str, data: dict) -> None:
    """분석 결과를 {youtube_video_id}.json 파일로 저장한다."""
    RESULTS_DIR.mkdir(exist_ok=True)
    with open(_json_path(youtube_video_id), "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)


def _delete_audio(audio_path: str) -> None:
    """분석 완료 후 오디오 파일을 삭제한다."""
    try:
        os.remove(audio_path)
        logger.info("오디오 파일 삭제 완료: %s", audio_path)
    except OSError as e:
        # 삭제 실패는 치명적이지 않으므로 경고만 기록
        logger.warning("오디오 파일 삭제 실패 (%s): %s", audio_path, e)


@router.post("/analyze", response_model=AnalyzeResponse)
async def analyze_music(request: AnalyzeRequest):
    """
    YouTube 영상의 음악 feature를 분석한다.

    동작 흐름:
        1. {video_id}.json 이 이미 존재하면 SKIPPED 반환 (경고 로그 출력)
        2. duration_seconds < 120 이면 SKIPPED 반환 (다운로드/분석 없음)
        3. yt-dlp로 오디오 다운로드
        4. Essentia로 분석
        5. 결과를 {video_id}.json 으로 저장
        6. 오디오 파일 삭제
        7. AnalyzeResponse 반환
    """

    # ── 1. 이미 분석된 JSON이 존재하면 건너뜀 ────────────────────────────────
    if _json_path(request.youtube_video_id).exists():
        logger.warning(
            "[SKIP] 이미 분석 결과 JSON이 존재합니다: %s",
            _json_path(request.youtube_video_id),
        )
        return AnalyzeResponse(
            youtube_video_id=request.youtube_video_id,
            title=request.title,
            status="SKIPPED",
            message=f"이미 분석 결과가 존재합니다 ({request.youtube_video_id}.json). 중복 분석을 건너뜁니다.",
            duration_seconds=request.duration_seconds,
        )

    # ── 2. 재생 시간 검사 (0은 미확인 상태이므로 건너뜀) ──────────────────────
    if 0 < request.duration_seconds < MIN_DURATION_SECONDS:
        return AnalyzeResponse(
            youtube_video_id=request.youtube_video_id,
            title=request.title,
            status="SKIPPED",
            message=f"재생 시간({request.duration_seconds}초)이 {MIN_DURATION_SECONDS}초 미만이므로 분석을 건너뜁니다.",
            duration_seconds=request.duration_seconds,
        )

    if request.duration_seconds > MAX_DURATION_SECONDS:
        return AnalyzeResponse(
            youtube_video_id=request.youtube_video_id,
            title=request.title,
            status="SKIPPED",
            message=f"재생 시간({request.duration_seconds}초)이 {MAX_DURATION_SECONDS}초를 초과하므로 분석을 건너뜁니다.",
            duration_seconds=request.duration_seconds,
        )

    # ── 3. 오디오 다운로드 ────────────────────────────────────────────────────
    try:
        audio_path = download_service.download_audio(
            youtube_url=request.youtube_url,
            youtube_video_id=request.youtube_video_id,
        )
        logger.info("다운로드 완료: %s", audio_path)
    except RuntimeError as e:
        return AnalyzeResponse(
            youtube_video_id=request.youtube_video_id,
            title=request.title,
            status="FAILED",
            message=f"다운로드 실패: {e}",
            duration_seconds=request.duration_seconds,
        )

    # ── 4. Essentia 분석 ──────────────────────────────────────────────────────
    try:
        features = essentia_service.analyze_audio(audio_path)
        logger.info("분석 완료: %s", request.youtube_video_id)
    except RuntimeError as e:
        # 분석 실패 시에도 오디오 파일은 삭제하여 디스크 낭비 방지
        _delete_audio(audio_path)
        return AnalyzeResponse(
            youtube_video_id=request.youtube_video_id,
            title=request.title,
            status="FAILED",
            message=f"분석 실패: {e}",
            duration_seconds=request.duration_seconds,
        )

    # ── 4.5. 임베딩 생성 (오디오 삭제 전) ────────────────────────────────────
    ef = features["essentia_features"]
    embedding = None
    try:
        embedding = embedding_service.create_embedding(
            bpm=features["bpm"],
            energy=features["energy"],
            danceability=features["danceability"],
            loudness=features["loudness"],
            musical_key=features["musical_key"],
            musical_scale=features["musical_scale"],
            spectral_centroid=ef.get("spectral_centroid", 0.0),
            zero_crossing_rate=ef.get("zero_crossing_rate", 0.0),
        )
        logger.info("임베딩 생성 완료: %s (%dD)", request.youtube_video_id, len(embedding))
    except Exception as e:
        logger.warning("임베딩 생성 실패 (Qdrant 미저장): %s", e)

    # ── 5. 결과 JSON 저장 ─────────────────────────────────────────────────────
    result_data = {
        "youtube_video_id": request.youtube_video_id,
        "title": request.title,
        "status": "COMPLETED",
        "message": "분석 완료",
        "duration_seconds": features["duration_seconds"],
        "bpm": features["bpm"],
        "energy": features["energy"],
        "danceability": features["danceability"],
        "loudness": features["loudness"],
        "musical_key": features["musical_key"],
        "musical_scale": features["musical_scale"],
        "spectral_centroid": ef.get("spectral_centroid"),
        "zero_crossing_rate": ef.get("zero_crossing_rate"),
        "embedding": embedding,
        "essentia_features": features["essentia_features"],
    }

    try:
        _save_result_json(request.youtube_video_id, result_data)
        logger.info("결과 JSON 저장 완료: %s", _json_path(request.youtube_video_id))
    except OSError as e:
        logger.error("결과 JSON 저장 실패 (%s): %s", request.youtube_video_id, e)

    # ── 6. 오디오 파일 삭제 ───────────────────────────────────────────────────
    _delete_audio(audio_path)

    # ── 7. 응답 반환 ──────────────────────────────────────────────────────────
    return AnalyzeResponse(
        youtube_video_id=request.youtube_video_id,
        title=request.title,
        status="COMPLETED",
        message="분석 완료",
        audio_path=None,
        duration_seconds=features["duration_seconds"],
        bpm=features["bpm"],
        energy=features["energy"],
        danceability=features["danceability"],
        loudness=features["loudness"],
        musical_key=features["musical_key"],
        musical_scale=features["musical_scale"],
        spectral_centroid=ef.get("spectral_centroid"),
        zero_crossing_rate=ef.get("zero_crossing_rate"),
        embedding=embedding,
        essentia_features=features["essentia_features"],
    )
