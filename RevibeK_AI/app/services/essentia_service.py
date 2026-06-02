import os
import numpy as np

# Essentia는 Linux/Mac 환경에서 주로 동작한다.
# Windows 개발 환경에서 import 에러가 나지 않도록 예외 처리.
try:
    import essentia
    import essentia.standard as es
    ESSENTIA_AVAILABLE = True
except ImportError:
    ESSENTIA_AVAILABLE = False


# ── 내부 유틸 ──────────────────────────────────────────────────────────────────

def _to_python(val):
    """
    numpy 스칼라/배열을 JSON 직렬화 가능한 Python 기본 타입으로 재귀 변환한다.
    Pydantic 응답 직렬화 전에 모든 Essentia 출력에 적용해야 한다.
    """
    if isinstance(val, np.integer):
        return int(val)
    if isinstance(val, np.floating):
        return float(val)
    if isinstance(val, np.ndarray):
        return val.tolist()
    if isinstance(val, dict):
        return {k: _to_python(v) for k, v in val.items()}
    if isinstance(val, (list, tuple)):
        return [_to_python(v) for v in val]
    return val


# ── 공개 API ───────────────────────────────────────────────────────────────────

def analyze_audio(audio_path: str) -> dict:
    """
    오디오 파일을 Essentia로 분석해 음악 feature를 반환한다.

    반환 키:
        duration_seconds (int)   : 실제 재생 시간(초)
        bpm              (float) : 템포 (BPM)
        energy           (float) : 에너지 [0, 1] 정규화
        danceability     (float) : 댄서빌리티 [0, 1] 정규화 (원본 범위 0–3)
        loudness         (float) : 라우드니스 (dB, 음수)
        musical_key      (str)   : 음악 키 (예: "C")
        musical_scale    (str)   : 음악 스케일 ("major" | "minor")
        essentia_features (dict) : 전체 분석 결과 (JSON 직렬화 가능)

    :param audio_path: 분석할 오디오 파일 경로
    :return: 위 키를 포함하는 dict
    :raises RuntimeError: Essentia 미설치 또는 분석 중 예외 발생 시
    """
    if not ESSENTIA_AVAILABLE:
        raise RuntimeError(
            "Essentia가 설치되어 있지 않습니다. "
            "`pip install essentia` 후 재시도하세요."
        )

    if not os.path.exists(audio_path):
        raise RuntimeError(f"오디오 파일이 존재하지 않습니다: {audio_path}")

    try:
        # ── 1. 오디오 로드 (44100 Hz 모노) ────────────────────────────────────
        loader = es.MonoLoader(filename=audio_path, sampleRate=44100)
        audio = loader()

        sample_rate = 44100
        duration = float(len(audio)) / sample_rate

        # ── 2. BPM / 리듬 분석 ────────────────────────────────────────────────
        # multifeature 방법이 단일 방법보다 정확도가 높다.
        rhythm = es.RhythmExtractor2013(method="multifeature")
        bpm, beats, beats_confidence, _, beats_intervals = rhythm(audio)

        # ── 3. 키 / 스케일 분석 ───────────────────────────────────────────────
        key_extractor = es.KeyExtractor()
        musical_key, musical_scale, key_strength = key_extractor(audio)

        # ── 4. 에너지 분석 ────────────────────────────────────────────────────
        # Energy()는 sum(x^2)/N 을 반환한다. [-1,1] 범위 신호라면 보통 0~1 사이.
        energy_raw = float(es.Energy()(audio))
        # 안전하게 [0, 1] 클리핑
        energy = min(energy_raw, 1.0)

        # ── 5. 라우드니스 분석 ────────────────────────────────────────────────
        # Loudness()는 라우드니스를 dB 단위(음수)로 반환한다.
        loudness = float(es.Loudness()(audio))

        # ── 6. 댄서빌리티 분석 ───────────────────────────────────────────────
        # Danceability()의 출력 범위는 0~3. [0, 1]로 정규화한다.
        danceability_raw, _ = es.Danceability(sampleRate=sample_rate)(audio)
        danceability = min(float(danceability_raw) / 3.0, 1.0)

        # ── 7. 추가 feature: SpectralCentroid, ZeroCrossingRate ──────────────
        # 프레임 단위 분석이 필요한 알고리즘들
        frame_size = 2048
        hop_size = 512

        windowing = es.Windowing(type="hann")
        spectrum_algo = es.Spectrum()
        centroid_algo = es.SpectralCentroidTime()
        zcr_algo = es.ZeroCrossingRate()

        centroids = []
        zcrs = []

        for frame in es.FrameGenerator(audio, frameSize=frame_size, hopSize=hop_size):
            centroids.append(float(centroid_algo(frame)))
            zcrs.append(float(zcr_algo(frame)))

        spectral_centroid = float(np.mean(centroids)) if centroids else 0.0
        zero_crossing_rate = float(np.mean(zcrs)) if zcrs else 0.0

        # ── 8. essentia_features: JSON 직렬화 가능한 전체 결과 dict ──────────
        essentia_features = _to_python({
            "duration": duration,
            "bpm": bpm,
            "beats_count": len(beats),
            "beats_confidence": beats_confidence,
            "key": musical_key,
            "scale": musical_scale,
            "key_strength": key_strength,
            "energy_raw": energy_raw,
            "energy": energy,
            "loudness_db": loudness,
            "danceability_raw": danceability_raw,
            "danceability": danceability,
            "spectral_centroid": spectral_centroid,
            "zero_crossing_rate": zero_crossing_rate,
        })

        return {
            "duration_seconds": int(duration),
            "bpm": round(float(bpm), 4),
            "energy": round(energy, 4),
            "danceability": round(danceability, 4),
            "loudness": round(loudness, 4),
            "musical_key": str(musical_key),
            "musical_scale": str(musical_scale),
            "essentia_features": essentia_features,
        }

    except RuntimeError:
        # 위에서 직접 발생시킨 RuntimeError는 그대로 전파
        raise
    except Exception as e:
        raise RuntimeError(f"Essentia 분석 중 오류 발생 ({audio_path}): {e}")
