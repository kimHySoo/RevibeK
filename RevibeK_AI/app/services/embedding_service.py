import math
from typing import List

_KEY_INDEX = {
    'C': 0, 'C#': 1, 'D': 2, 'D#': 3, 'E': 4, 'F': 5,
    'F#': 6, 'G': 7, 'G#': 8, 'A': 9, 'A#': 10, 'B': 11
}


def _clamp(v: float) -> float:
    return max(0.0, min(1.0, v))


def create_embedding(
    bpm: float,
    energy: float,
    danceability: float,
    loudness: float,
    musical_key: str,
    musical_scale: str,
    spectral_centroid: float,
    zero_crossing_rate: float,
) -> List[float]:
    """
    librosa 분석 결과에서 9차원 임베딩 벡터 생성.
    Spring Boot SongVectorUtil.toVector()와 동일한 구조로 호환성 보장.

    차원 구성:
        [0] bpm / 300
        [1] energy
        [2] danceability
        [3] (loudness + 60) / 60
        [4] sin(key * π/6)
        [5] cos(key * π/6)
        [6] 1.0 if minor else 0.0
        [7] spectral_centroid / 8000
        [8] zero_crossing_rate
    """
    key_idx = _KEY_INDEX.get(musical_key, 0)
    key_rad = key_idx * math.pi / 6.0

    return [
        _clamp(bpm / 300.0),
        _clamp(energy),
        _clamp(danceability),
        _clamp((loudness + 60.0) / 60.0),
        float(math.sin(key_rad)),
        float(math.cos(key_rad)),
        1.0 if musical_scale.lower() == 'minor' else 0.0,
        _clamp(spectral_centroid / 8000.0),
        _clamp(zero_crossing_rate),
    ]
