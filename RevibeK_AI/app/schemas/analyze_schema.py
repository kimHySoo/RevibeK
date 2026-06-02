from pydantic import BaseModel
from typing import Optional


class AnalyzeRequest(BaseModel):
    """Spring Boot에서 FastAPI로 전달하는 분석 요청 DTO"""
    youtube_video_id: str
    youtube_url: str
    title: str
    duration_seconds: int


class AnalyzeResponse(BaseModel):
    """FastAPI가 Spring Boot로 반환하는 분석 결과 DTO"""
    youtube_video_id: str
    title: str
    # COMPLETED | SKIPPED | FAILED
    status: str
    message: str
    audio_path: Optional[str] = None
    duration_seconds: int
    bpm: Optional[float] = None
    energy: Optional[float] = None
    danceability: Optional[float] = None
    loudness: Optional[float] = None
    musical_key: Optional[str] = None
    musical_scale: Optional[str] = None
    # Essentia로 추출한 전체 feature (JSON 직렬화 가능)
    essentia_features: Optional[dict] = None
